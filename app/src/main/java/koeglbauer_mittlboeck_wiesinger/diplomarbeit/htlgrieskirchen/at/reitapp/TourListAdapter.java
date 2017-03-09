package koeglbauer_mittlboeck_wiesinger.diplomarbeit.htlgrieskirchen.at.reitapp;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TourListAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] itemname;
    private final Integer[] imgid;
    private final String[] itemdesc;
    private final String[] itemnumb;
    private final Integer[] backgroundid;

    public TourListAdapter(Activity context, String[] itemname, Integer[] imgid, String[] itemdesc, String[] itemnumb, Integer[] backgroundid)
    {
        super(context, R.layout.tour_list, itemname);
        this.context=context;
        this.itemname=itemname;
        this.imgid=imgid;
        this.itemdesc=itemdesc;
        this.itemnumb=itemnumb;
        this.backgroundid = backgroundid;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.tour_list, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.tourListNameView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.tourListImageView);
        TextView txtDescription = (TextView) rowView.findViewById(R.id.tourListRangeView);
        View layout=rowView.findViewById(R.id.tourListLayoutView);

        layout.setBackgroundResource(backgroundid[position]);
        txtTitle.setText(itemname[position]);
        imageView.setImageResource(imgid[position]);
        txtDescription.setText(itemdesc[position]);

        return rowView;
    };
}
