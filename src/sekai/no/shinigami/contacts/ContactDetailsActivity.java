package sekai.no.shinigami.contacts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ContactDetailsActivity extends FragmentActivity {

	public static final String EXTRA_CONTENT_LOOKUP_KEY = "ContactDetailsActivity.ContactLookupKey";
	public static final String EXTRA_CONTACT = "ContactDetailsActivity.Contact";

	private Contact mContact;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_details);

		Intent i = getIntent();
		if (i.hasExtra(EXTRA_CONTACT)) {
			mContact = i.getParcelableExtra(EXTRA_CONTACT);
			displayContactHeader();
			displayContactDetails();
		} else {
			if (i.hasExtra(EXTRA_CONTENT_LOOKUP_KEY)) {
				String lookupKey = i.getStringExtra(EXTRA_CONTENT_LOOKUP_KEY);
				displayContactHeader(lookupKey);
				displayContactDetails(lookupKey);
			}
		}
	}

	private static final String[] FROM_COLUMNS_PHONE = { Phone.NUMBER };

	@SuppressLint("InlinedApi")
	private static final String[] FROM_COLUMNS_EMAIL = { ContactUtils
			.hasHoneycomb() ? Email.ADDRESS : Email.DATA1 };

	private static final int[] TO_VIEWS = { R.id.text1 };

	private void displayContactHeader() {
		((TextView) findViewById(R.id.textViewName))
				.setText(mContact.getName());
		((ImageView) findViewById(R.id.imageViewPhoto)).setImageBitmap(mContact
				.getPhotoBitmap());
	}

	private void displayContactHeader(String lookupKey) {
		((TextView) findViewById(R.id.textViewName)).setText(ContactUtils
				.queryName(getContentResolver(), lookupKey));
		((ImageView) findViewById(R.id.imageViewPhoto))
				.setImageBitmap(ContactUtils.queryThumbnailPhoto(
						getContentResolver(), lookupKey));
	}

	/*
	 * For both the phone and email ListViews, set an ArrayAdapter which will
	 * populate each one of them with the corresponding data from mContact
	 */
	private void displayContactDetails() {
		ListView lv = (ListView) findViewById(R.id.listViewPhone);
		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item,
				R.id.text1, mContact.getPhoneList()));

		lv = (ListView) findViewById(R.id.listViewEmail);
		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item,
				R.id.text1, mContact.getEmailList()));
	}

	/*
	 * For both the phone and email ListViews, set a CursorAdapter which will
	 * populate each one of them with the corresponding data from the contacts
	 * database
	 */
	private void displayContactDetails(String contactLookupKey) {
		Cursor c = ContactUtils.queryPhone(getContentResolver(),
				contactLookupKey);
		ListView lv = (ListView) findViewById(R.id.listViewPhone);
		lv.setAdapter(new SimpleCursorAdapter(this, R.layout.simple_list_item,
				c, FROM_COLUMNS_PHONE, TO_VIEWS, 0));

		c = ContactUtils.queryEmail(getContentResolver(), contactLookupKey);
		lv = (ListView) findViewById(R.id.listViewEmail);
		lv.setAdapter(new SimpleCursorAdapter(this, R.layout.simple_list_item,
				c, FROM_COLUMNS_EMAIL, TO_VIEWS, 0));
	}
}
