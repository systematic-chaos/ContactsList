package sekai.no.shinigami.contacts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

public class Contact implements Parcelable {
	private String mName;
	private Bitmap mPhotoBitmap;
	private List<String> mPhoneList;
	private List<String> mEmailList;
	private String mLookupKey;

	public Contact(String name) {
		mName = name;
	}

	public Contact(String name, ContentResolver cr, Uri photoUri) {
		mName = name;
		try {
			mPhotoBitmap = MediaStore.Images.Media.getBitmap(cr, photoUri);
		} catch (IOException ioe) {
			Log.e("CONTACT", "IOException getting Bitmap from Uri");
		}
		mPhoneList = new ArrayList<String>();
		mEmailList = new ArrayList<String>();
	}

	public Contact(String name, Bitmap photoBitmap) {
		mName = name;
		mPhotoBitmap = photoBitmap;
		mPhoneList = new ArrayList<String>();
		mEmailList = new ArrayList<String>();
	}

	public boolean addPhone(String phone) {
		return mPhoneList.add(phone);
	}

	public boolean addEmail(String email) {
		return mEmailList.add(email);
	}

	public String getName() {
		return mName;
	}

	public Bitmap getPhotoBitmap() {
		return mPhotoBitmap;
	}

	public List<String> getPhoneList() {
		return mPhoneList;
	}

	public List<String> getEmailList() {
		return mEmailList;
	}

	public String getLookupKey() {
		return mLookupKey;
	}

	public boolean hasPhotoBitmap() {
		return mPhotoBitmap != null;
	}

	public boolean hasPhone() {
		return !mPhoneList.isEmpty();
	}

	public boolean hasEmail() {
		return !mEmailList.isEmpty();
	}

	public int getPhoneCount() {
		return mPhoneList.size();
	}

	public int getEmailCount() {
		return mEmailList.size();
	}

	public void setName(String name) {
		mName = name;
	}

	public void setPhotoBitmap(ContentResolver cr, Uri photoUri) {
		mPhotoBitmap = ContactUtils.getBitmapFromUri(cr, photoUri);
	}

	public void setPhotoBitmap(Bitmap photoBitmap) {
		mPhotoBitmap = photoBitmap;
	}

	public void setPhoneList(List<String> phoneList) {
		mPhoneList = new ArrayList<String>(phoneList);
	}

	public void setEmailList(List<String> emailList) {
		mEmailList = new ArrayList<String>(emailList);
	}

	public void setLookupKey(String lookupKey) {
		mLookupKey = lookupKey;
	}

	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {

		@Override
		public Contact createFromParcel(Parcel source) {
			return new Contact(source);
		}

		@Override
		public Contact[] newArray(int size) {
			return new Contact[size];
		}
	};

	private Contact(Parcel source) {
		readFromParcel(source);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeParcelable(mPhotoBitmap, flags);
		dest.writeStringList(mPhoneList);
		dest.writeStringList(mEmailList);
	}

	public void readFromParcel(Parcel source) {
		mName = source.readString();
		mPhotoBitmap = source.readParcelable(Bitmap.class.getClassLoader());
		mPhoneList = source.createStringArrayList();
		mEmailList = source.createStringArrayList();
	}
}
