package sekai.no.shinigami.contacts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

public class ContactUtils {

	/*
	 * The projection for the CursorLoader query. This is a list of columns that
	 * the Contacts Provider should return in the Cursor.
	 */
	@SuppressLint("InlinedApi")
	private static final String[] PROJECTION_CONTACTS = {
			/*
			 * The detail data row ID. To make a ListView work, this column is
			 * required.
			 */
			Contacts._ID,

			/* The contact's LOOKUP_KEY, to construct a content URI */
			Contacts.LOOKUP_KEY,

			/* The primary display name */
			ContactUtils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY
					: Contacts.DISPLAY_NAME,

			/*
			 * The contact's picture to be displayed aside with the
			 * corresponding name
			 */
			Contacts.PHOTO_THUMBNAIL_URI,

			/*
			 * An indicator of whether the contact has a phone number associated
			 * with her
			 */
			Contacts.HAS_PHONE_NUMBER, };

	// Defines the selection clause
	private static final String SELECTION_CONTACTS_LOOKUP_KEY = Contacts.LOOKUP_KEY
			+ "=?";
	private static final String SELECTION_CONTACTS_HAS_PHONE_NUMBER = Contacts.HAS_PHONE_NUMBER
			+ "= ?";

	public enum ContactIndexes {
		_ID, LOOKUP_KEY, DISPLAY_NAME, PHOTO_THUMBNAIL_URI, HAS_PHONE_NUMBER
	};

	/*
	 * The desired sort order for the returned Cursor. Defines a string that
	 * specifies a sort order of MIME type
	 */
	@SuppressLint("InlinedApi")
	private static final String SORT_ORDER_CONTACTS = (ContactUtils
			.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY
			: Contacts.DISPLAY_NAME)
			+ " ASC ";

	public static CursorLoader queryContactsList(Context context,
			boolean onlyPhone) {
		// Starts the query providing the defined selection criteria
		String[] selectionArgs;
		String selectionContacts;
		if (onlyPhone) {
			selectionArgs = new String[1];
			selectionArgs[0] = "1";
			selectionContacts = SELECTION_CONTACTS_HAS_PHONE_NUMBER;
		} else {
			selectionArgs = null;
			selectionContacts = null;
		}
		return new CursorLoader(context, Contacts.CONTENT_URI,
				PROJECTION_CONTACTS, selectionContacts, selectionArgs,
				SORT_ORDER_CONTACTS);
	}

	private static final String[] PROJECTION_PHONE = { Data._ID, Phone.NUMBER,
			Phone.TYPE, Phone.LABEL };

	private static final String SELECTION_PHONE = Data.LOOKUP_KEY + "=?"
			+ " AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'";

	@SuppressLint("InlinedApi")
	private static final String[] PROJECTION_EMAIL = { Data._ID,
			hasHoneycomb() ? Email.ADDRESS : Email.DATA1, Email.TYPE,
			Email.LABEL };

	private static final String SELECTION_EMAIL = Data.LOOKUP_KEY + "=?"
			+ " AND " + Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'";

	public enum ContactDetailsIndexes {
		_ID, NUMBER_ADDRESS, TYPE, LABEL
	};

	public static String queryName(ContentResolver contentResolver,
			String lookupKey) {
		String displayName;
		String[] selectionArgs = { lookupKey };
		Cursor c = contentResolver.query(Contacts.CONTENT_URI,
				PROJECTION_CONTACTS, SELECTION_CONTACTS_LOOKUP_KEY,
				selectionArgs, null);
		displayName = c.getString(ContactIndexes.DISPLAY_NAME.ordinal());
		c.close();
		return displayName;
	}

	public static Bitmap queryThumbnailPhoto(ContentResolver contentResolver,
			String lookupKey) {
		Bitmap thumbnailPhotoBitmap = null;
		String[] selectionArgs = { lookupKey };
		Cursor c = contentResolver.query(Contacts.CONTENT_URI,
				PROJECTION_CONTACTS, SELECTION_CONTACTS_LOOKUP_KEY,
				selectionArgs, null);
		if (!c.isNull(ContactIndexes.PHOTO_THUMBNAIL_URI.ordinal())) {
			Uri thumbnailPhotoUri = Uri.parse(c
					.getString(ContactIndexes.PHOTO_THUMBNAIL_URI.ordinal()));
			thumbnailPhotoBitmap = getBitmapFromUri(contentResolver,
					thumbnailPhotoUri);
		}
		c.close();
		return thumbnailPhotoBitmap;
	}

	public static Cursor queryPhone(ContentResolver contentResolver,
			String lookupKey) {
		String[] selectionArgs = { lookupKey };
		return contentResolver.query(Data.CONTENT_URI, PROJECTION_PHONE,
				SELECTION_PHONE, selectionArgs, null);
	}

	public static Cursor queryEmail(ContentResolver contentResolver,
			String lookupKey) {
		String[] selectionArgs = { lookupKey };
		return contentResolver.query(Data.CONTENT_URI, PROJECTION_EMAIL,
				SELECTION_EMAIL, selectionArgs, null);
	}

	public static List<String> queryPhoneList(ContentResolver contentResolver,
			String lookupKey) {
		List<String> phoneList = new ArrayList<String>();
		Cursor phoneCursor = queryPhone(contentResolver, lookupKey);
		while (phoneCursor.moveToNext()) {
			phoneList.add(phoneCursor
					.getString(ContactDetailsIndexes.NUMBER_ADDRESS.ordinal()));
		}
		phoneCursor.close();
		return phoneList;
	}

	public static List<String> queryEmailList(ContentResolver contentResolver,
			String lookupKey) {
		List<String> emailList = new ArrayList<String>();
		Cursor emailCursor = queryEmail(contentResolver, lookupKey);
		while (emailCursor.moveToNext()) {
			emailList.add(emailCursor
					.getString(ContactDetailsIndexes.NUMBER_ADDRESS.ordinal()));
		}
		emailCursor.close();
		return emailList;
	}

	/**
	 * Uses static final constants to detect if the device's platform version is
	 * Honeycomb or later.
	 */
	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasJellybean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

	public static Bitmap getBitmapFromUri(ContentResolver cr, Uri uri) {
		Bitmap bm = null;
		try {
			bm = MediaStore.Images.Media.getBitmap(cr, uri);
		} catch (IOException ioe) {
			Log.e("ContactUtils", "IOException: " + ioe.getMessage());
		}
		return bm;
	}
}
