package sekai.no.shinigami.contacts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sekai.no.shinigami.contacts.ContactUtils.ContactIndexes;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;

public class ContactsListActivity extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

	private static final int ID_LOADER = 0;
	private static final String EXPORT_PATH = "contacts.cla";

	/*
	 * Defines an array that contains column names to move from the Cursor to
	 * the Listview.
	 */
	@SuppressLint("InlinedApi")
	private final static String[] FROM_COLUMNS = {
			ContactUtils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY
					: Contacts.DISPLAY_NAME, Contacts.PHOTO_THUMBNAIL_URI };

	/*
	 * Defines an array that contains resource ids for the layout views that get
	 * the Cursor column contents. The id is pre-defined in the Android
	 * framework, so it is prefaced with "android.R.id"
	 */
	private final static int[] TO_IDS = { R.id.text1, R.id.image1 };

	// Define global mutable variables

	// Define a ListView object
	private ListView mContactsList;

	// An adapter that binds the result Cursor to the ListView
	private CheckableCursorAdapter mCursorAdapter;

	// Progress bar to be displayed while the contacts results are being queried
	private ProgressBar mProgressBar;

	private boolean mHasPhone;

	// Empty public constructor, required by the system
	public ContactsListActivity() {
	}

	/*
	 * Invoked when the Activity is about to be instantiated and its UI drawn.
	 * Put initialization steps here.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Always call the super method first
		super.onCreate(savedInstanceState);

		setContentView(R.layout.contacts_list_layout);

		// Gets the ListView from the View list
		mContactsList = (ListView) findViewById(android.R.id.list);

		// Gets the progress bar to show the loader current status
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		// Gets a CursorAdapter
		mCursorAdapter = new CheckableCursorAdapter(this,
				R.layout.contacts_list_item, null, FROM_COLUMNS, TO_IDS, 0);

		// Sets the adapter for the ListView
		mContactsList.setAdapter(mCursorAdapter);

		// Set the item click listener to be the current fragment.
		mContactsList.setOnItemClickListener(this);

		// CheckBox used for select or deselect all the contacts
		final CheckBox allContactsCheckBox = (CheckBox) findViewById(R.id.checkBoxAllContacts);
		allContactsCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						CheckBox check;
						for (int n = 0; n < mCursorAdapter.getCount(); n++) {
							check = (CheckBox) mCursorAdapter.getView(n, null,
									null).findViewById(R.id.check1);
							check.setChecked(isChecked);
						}
						mCursorAdapter.notifyDataSetChanged();
					}
				});

		// CheckBox which will discriminate who of the contacts are displayed
		CheckBox hasPhoneCheckBox = (CheckBox) findViewById(R.id.checkBoxHasPhone);
		mHasPhone = hasPhoneCheckBox.isChecked();
		hasPhoneCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mHasPhone = isChecked;
						allContactsCheckBox.setChecked(true);
						getSupportLoaderManager().restartLoader(ID_LOADER,
								null, ContactsListActivity.this);
					}
				});

		// Button used to export the list of selected contacts along with their
		// detailed information
		Button exportContactsButton = (Button) findViewById(R.id.buttonExportContacts);
		exportContactsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				List<Contact> contactsExported = queryContactsDetailedList(true);
				exportContactsListToFile(contactsExported, EXPORT_PATH);
				sendFile(EXPORT_PATH);
			}
		});

		// Initializes the loader framework
		getSupportLoaderManager().initLoader(ID_LOADER, null, this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View item, int position,
			long rowID) {
		// Get the cursor
		Cursor cursor = mCursorAdapter.getCursor();

		// Move to the selected contact
		cursor.moveToPosition(position);

		// Create the contact
		Contact contact = queryContactDetails(position);

		Intent i = new Intent(this, ContactDetailsActivity.class);
		i.putExtra(ContactDetailsActivity.EXTRA_CONTENT_LOOKUP_KEY,
				contact.getLookupKey());
		i.putExtra(ContactDetailsActivity.EXTRA_CONTACT, contact);
		startActivity(i);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		mProgressBar.setVisibility(View.VISIBLE);
		return ContactUtils.queryContactsList(this, mHasPhone);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mProgressBar.setVisibility(View.GONE);
		/*
		 * Process the resulting Cursor and put it in the adapter for the
		 * ListView
		 */
		mCursorAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		/*
		 * If you have current references to the Cursor, delete them
		 */
		mCursorAdapter.swapCursor(null);
	}

	private Contact queryContactDetails(int index) {
		Contact contact;
		Cursor contactsCursor = mCursorAdapter.getCursor();
		contactsCursor.moveToPosition(index);
		String lookupKey = contactsCursor.getString(ContactIndexes.LOOKUP_KEY
				.ordinal());
		String displayName = contactsCursor
				.getString(ContactIndexes.DISPLAY_NAME.ordinal());
		if (!contactsCursor
				.isNull(ContactIndexes.PHOTO_THUMBNAIL_URI.ordinal())) {
			Uri photoThumbnailUri = Uri.parse(contactsCursor
					.getString(ContactIndexes.PHOTO_THUMBNAIL_URI.ordinal()));
			contact = new Contact(displayName, getContentResolver(),
					photoThumbnailUri);
		} else {
			contact = new Contact(displayName);
		}
		contact.setLookupKey(lookupKey);
		contact.setPhoneList(ContactUtils.queryPhoneList(getContentResolver(),
				lookupKey));
		contact.setEmailList(ContactUtils.queryEmailList(getContentResolver(),
				lookupKey));
		return contact;
	}

	private List<Contact> queryContactsDetailedList(boolean checked) {
		List<Contact> contactsDetails = new ArrayList<Contact>();
		for (int n = 0; n < mCursorAdapter.getCount(); n++) {
			CheckBox checkedContact = (CheckBox) mCursorAdapter.getView(n,
					null, null).findViewById(R.id.check1);
			if (checkedContact.isChecked() || !checked) {
				contactsDetails.add(queryContactDetails(n));
			}
		}
		return contactsDetails;
	}

	private void exportContactsListToFile(List<Contact> contacts, String path) {
		FileOutputStream fos = null;
		try {
			// fos = openFileOutput(path, Context.MODE_WORLD_READABLE);
			fos = openFileOutput(path, Context.MODE_PRIVATE);
		} catch (FileNotFoundException fnfe) {
			Log.e(getLocalClassName(),
					"FileNotFoundException: " + fnfe.getMessage());
		}
		if (fos != null) {
			Parcel p = Parcel.obtain();
			p.writeTypedList(contacts);
			try {
				fos.write(p.marshall());
				fos.flush();
				fos.close();
			} catch (IOException ioe) {
				Log.e(getLocalClassName(), "IOException: " + ioe.getMessage());
			} finally {
				p.recycle();
			}
		}
	}

	@SuppressLint("NewApi")
	private void sendFile(String path) {
		Intent i = new Intent(Intent.ACTION_SEND);
		String description = getString(R.string.activity_contacts_list) + " - "
				+ getString(R.string.intent_chooser);
		// Uri data = Uri.parse(Uri.encode(getFilesDir().toString() + "/" +
		// path));
		Uri data = Uri.parse(Uri.encode(getFileStreamPath(path).toString()));
		i.setType("application/vnd.contact.cmsg");
		if (ContactUtils.hasJellybean()) {
			String[] mimeTypes = { "*/*" };
			i.setClipData(new ClipData(new ClipDescription(description,
					mimeTypes), new ClipData.Item(data)));
			i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		i.putExtra(Intent.EXTRA_STREAM, data);
		startActivity(Intent.createChooser(i,
				getString(R.string.intent_chooser)));
	}

	public void importContactsListFromFile(String path) {
		List<Contact> contacts = null;
		FileInputStream fis = null;
		try {
			fis = openFileInput(path);
		} catch (FileNotFoundException fnfe) {
			Log.e(getLocalClassName(), fnfe.getMessage());
		}
		if (fis != null) {
			Parcel p = Parcel.obtain();
			try {
				byte[] marshallBuffer = org.apache.commons.io.IOUtils
						.toByteArray(fis);
				p.unmarshall(marshallBuffer, 0, marshallBuffer.length);
				contacts = p.createTypedArrayList(Contact.CREATOR);
				fis.close();
			} catch (IOException ioe) {
				Log.e(getLocalClassName(), "IOException: " + ioe.getMessage());
			} finally {
				p.recycle();
			}
		}
		if (contacts != null) {
			insertContacts(contacts);
		}
	}

	@SuppressLint("InlinedApi")
	private void insertContacts(List<Contact> contactsList) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		for (int i = 0; i < contactsList.size(); i++) {
			Contact rawContact = contactsList.get(i);
			int rawContactInsertIndex = ops.size();
			ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
					.withValue(RawContacts.ACCOUNT_TYPE, null)
					.withValue(RawContacts.ACCOUNT_NAME, null).build());
			ops.add(ContentProviderOperation
					.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID,
							rawContactInsertIndex)
					.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
					.withValue(StructuredName.DISPLAY_NAME,
							rawContact.getName()).build());
			if (rawContact.hasPhotoBitmap()) {
				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
						.withValue(Photo.PHOTO, rawContact.getPhotoBitmap())
						.build());
			}
			List<String> rawContactData = rawContact.getPhoneList();
			for (int j = 0; j < rawContactData.size(); j++) {
				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
						.withValue(Phone.NUMBER, rawContactData.get(j)).build());
			}
			rawContactData = rawContact.getEmailList();
			for (int j = 0; j < rawContactData.size(); j++) {
				ops.add(ContentProviderOperation
						.newInsert(Data.CONTENT_URI)
						.withValueBackReference(Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
						.withValue(
								ContactUtils.hasHoneycomb() ? Email.ADDRESS
										: Email.DATA1, rawContactData.get(j))
						.build());
			}
			rawContact.setLookupKey(null);
		}
		try {
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (OperationApplicationException oae) {
			Log.e(getLocalClassName(),
					"OperationApplicationException: " + oae.getMessage());
		} catch (RemoteException re) {
			Log.e(getLocalClassName(), "RemoteException: " + re.getMessage());
		}
	}
}
