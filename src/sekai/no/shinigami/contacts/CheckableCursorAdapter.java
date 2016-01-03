package sekai.no.shinigami.contacts;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CheckableCursorAdapter extends SimpleCursorAdapter {

	private boolean[] mChecked;

	public CheckableCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
	}

	@Override
	public Cursor swapCursor(Cursor c) {
		Cursor oldCursor = super.swapCursor(c);
		if (c != null && c != oldCursor) {
			// Create an array to hold the checked state
			// Set array size to # of elements in cursor
			mChecked = new boolean[c.getCount()];
			// Loop through array and set initial state to checked
			for (int i = 0; i < c.getCount(); i++) {
				mChecked[i] = true;
			}
		}
		return oldCursor;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = super.getView(position, convertView, parent);

		// Set the state of the checkbox
		CompoundButton cb = (CompoundButton) row.findViewById(R.id.check1);
		cb.setOnCheckedChangeListener(null);
		cb.setChecked(mChecked[position]);
		cb.setTag(Integer.valueOf(position));
		cb.setOnCheckedChangeListener(mListener);
		return row;
	}

	private CompoundButton.OnCheckedChangeListener mListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			Integer realPosition = (Integer) buttonView.getTag();
			mChecked[realPosition] = isChecked;
		}
	};
}
