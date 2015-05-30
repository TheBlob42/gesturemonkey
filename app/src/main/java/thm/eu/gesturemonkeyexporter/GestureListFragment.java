package thm.eu.gesturemonkeyexporter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import thm.eu.gesturemonkey.Gesture;
import thm.eu.gesturemonkey.GestureMonkey;

/**
 * Created by Tobi on 13.01.2015.
 */
public class GestureListFragment extends Fragment implements ImportDialogFragment.ImportDialogListener{

    private GestureMonkey monkey;

    private Menu mMenu;

    private ListView lvGestures;
    private GestureListAdapter lvAdapter;
    private TextView tvNoEntries;

    private ArrayList<String> selectedGestures;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        setHasOptionsMenu(true);
        selectedGestures = new ArrayList<String>();

        monkey = GestureMonkey.getInstance();

        View view = inflater.inflate(R.layout.fragment_gesturelist, container, false);

        lvGestures = (ListView)view.findViewById(R.id.lvGestures);
        lvAdapter = new GestureListAdapter(getActivity(), monkey.getAllGestures().toArray(new Gesture[0]));
        lvGestures.setAdapter(lvAdapter);

        tvNoEntries = (TextView)view.findViewById(R.id.tvNoEntries);
        if(lvAdapter.getCount() > 0){
            tvNoEntries.setVisibility(View.GONE);
        }

        lvGestures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GestureListAdapter.ViewHolder holder = (GestureListAdapter.ViewHolder) view.getTag();
                boolean checked = holder.chkEdit.isChecked();

                //add or remove selected gesture to the vector of selected gestures
                if (checked) {
                    selectedGestures.remove(((Gesture)parent.getItemAtPosition(position)).name);
                } else {
                    selectedGestures.add(((Gesture)parent.getItemAtPosition(position)).name);
                }

                //draw the action bar buttons depending on how many items are selected (dirty!)
                mMenu.removeItem(R.id.action_select_all);
                mMenu.removeItem(R.id.action_import);
                mMenu.removeItem(R.id.action_add);
                mMenu.removeItem(R.id.action_delete);
                mMenu.removeItem(R.id.action_test);
                mMenu.removeItem(R.id.action_export);

                if (selectedGestures.size() > 0) {
                    getActivity().getMenuInflater().inflate(R.menu.gesturelist_selected, mMenu);
                } else {
                    getActivity().getMenuInflater().inflate(R.menu.gesturelist, mMenu);
                }

                //change check status of the CheckBox
                holder.chkEdit.setChecked(!checked);
            }
        });

        return view;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim){
        //only on app-start create an fade-in animator
        if(nextAnim == android.R.animator.fade_in){
            return ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).setDuration(300);
        }

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float displayWidth = size.x;

        Animator animator = null;
        //on enter let the fragment slide-in from the left
        if(enter) {
            animator = ObjectAnimator.ofFloat(this, "translationX", -displayWidth, 0);
        }
        //on leave fade-out the fragment
        else {
            animator = ObjectAnimator.ofFloat(this, "translationX", 0, -displayWidth);
        }
        animator.setDuration(300);

        return animator;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mMenu = menu;
        menuInflater.inflate(R.menu.gesturelist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_select_all){
            lvAdapter.selectAll();
            lvAdapter.notifyDataSetChanged();

            selectedGestures = lvAdapter.getAllGestureNames();

            if(selectedGestures.size() > 0){
                mMenu.removeItem(R.id.action_select_all);
                mMenu.removeItem(R.id.action_add);
                mMenu.removeItem(R.id.action_import);
                getActivity().getMenuInflater().inflate(R.menu.gesturelist_selected, mMenu);
            }
        }

        if(id == R.id.action_add) {
            NewGestureFragment newGestureFragment = new NewGestureFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, newGestureFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            return true;
        }

        if(id == R.id.action_test){
            TestGesturesFragment testGesturesFragment = new TestGesturesFragment();

            Bundle bundle = new Bundle();
            bundle.putStringArrayList("SelectedGestures", selectedGestures);
            testGesturesFragment.setArguments(bundle);

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, testGesturesFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            return true;
        }

        if(id == R.id.action_export){
            DialogFragment dialog = new ExportDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putStringArray("SelectedGestures", selectedGestures.toArray(new String[0]));
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "ExportDialog");
        }

        if(id == R.id.action_import){
            DialogFragment dialog = new ImportDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putStringArray("SelectedGestures", selectedGestures.toArray(new String[0]));
            dialog.setArguments(bundle);
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), "ImportDialog");
        }

        if(id == R.id.action_delete){
            //TODO Löschen einbauen
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onImportPositiveClick() {
        lvAdapter = new GestureListAdapter(getActivity(), monkey.getAllGestures().toArray(new Gesture[0]));
        lvGestures.setAdapter(lvAdapter);

        if(lvAdapter.getCount() > 0){
            tvNoEntries.setVisibility(View.GONE);
        }
    }
}
