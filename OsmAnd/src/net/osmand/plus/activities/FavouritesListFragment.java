/**
 * 
 */
package net.osmand.plus.activities;

import java.util.Comparator;
import java.util.List;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Intent;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * 
 */
public class FavouritesListFragment extends SherlockListFragment implements SearchActivityChild {

	public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY"; 
	public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;
	
	private FavouritesAdapter favouritesAdapter;

	private boolean selectFavoriteMode;
	private OsmandSettings settings;
	private LatLon location;
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		settings = ((OsmandApplication) getApplication()).getSettings();
		OsmandApplication app = (OsmandApplication) getApplication();
		favouritesAdapter = new FavouritesAdapter(activity, app.getFavorites().getFavouritePoints());
		setListAdapter(favouritesAdapter);
	}

	private OsmandApplication getApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getActivity().getIntent();
		if (intent != null) {
			selectFavoriteMode = intent.hasExtra(SELECT_FAVORITE_POINT_INTENT_KEY);
			if (intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)) {
				double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
				double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
				if (lat != 0 || lon != 0) {
					favouritesAdapter.location = new LatLon(lat, lon);
				}
			}
		}
		if (!isSelectFavoriteMode()) {
			if (location == null && getActivity() instanceof SearchActivity) {
				location = ((SearchActivity) getActivity()).getSearchPoint();
			}
			if (location == null) {
				location = settings.getLastKnownMapLocation();
			}
		}
		locationUpdate(location);
	}

	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		if (favouritesAdapter != null) {
			favouritesAdapter.updateLocation(l);
		}
	}
	
	public boolean isSelectFavoriteMode(){
		return selectFavoriteMode;
	}
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		if (!isSelectFavoriteMode()) {
			FavouritePoint point = favouritesAdapter.getItem(position);
			String name = getString(R.string.favorite) + ": " + point.getName();
			LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
			View.OnClickListener onshow = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					settings.SHOW_FAVORITES.set(true);							
				}
			};
			ContextMenuAdapter qa = new ContextMenuAdapter(v.getContext());
			qa.setAnchor(v);
			DirectionsDialogs.createDirectionsActions(qa, location, point, name, settings.getLastKnownMapZoom(), getActivity(),
					true, false);
			MapActivityActions.showObjectContextMenu(qa, getActivity(), onshow);
		} else {
			Intent intent = getActivity().getIntent();
			intent.putExtra(SELECT_FAVORITE_POINT_INTENT_KEY, favouritesAdapter.getItem(position));
			getActivity().setResult(SELECT_FAVORITE_POINT_RESULT_OK, intent);
			getActivity().finish();
		}
	}

	public static class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		private Activity activity;
		private LatLon location;
		private OsmandApplication app;
		
		public LatLon getLocation() {
			return location;
		}
		
		public void updateLocation(LatLon l) {
			location = l;
			sort(new Comparator<FavouritePoint>() {
				@Override
				public int compare(FavouritePoint object1, FavouritePoint object2) {
					if (location != null) {
						double d1 = MapUtils.getDistance(location, object1.getLatitude(), object1.getLongitude());
						double d2 = MapUtils.getDistance(location, object2.getLatitude(), object2.getLongitude());
						if (d1 == d2) {
							return 0;
						} else if (d1 > d2) {
							return 1;
						}
						return -1;
					} else {
						return getName(object1).compareTo(getName(object2));
					}
				}
			});			
		}

		public FavouritesAdapter(Activity activity, List<FavouritePoint> list) {
			super(activity, R.layout.favourites_list_item, list);
			this.activity = activity;
			this.app = ((OsmandApplication) activity.getApplication());
		}
		
		public String getName(FavouritePoint model){
			return model.getCategory() + " : " + model.getName();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = getItem(position);
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(activity, model.getColor()));
			String distance = "";
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), location.getLatitude(), location
						.getLongitude()));
				distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  " ;
			}
			
			label.setText(distance + getName(model), BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.color_distance)), 0, distance.length(), 0);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
			return row;
		}

	}


}
