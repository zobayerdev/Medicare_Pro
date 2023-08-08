package com.trodev.medicarepro.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.trodev.medicarepro.R;
import com.trodev.medicarepro.adapter.MedicineUserAdapter;
import com.trodev.medicarepro.models.MedicineData;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView dataRv;
    private List<MedicineData> dataList, filterDataList;
    private ProgressBar progressBar;
    private MedicineUserAdapter adapter;
    private DatabaseReference reference;
    private String lastKey = "";
    private int pageSize = 100;

    // Flag to prevent multiple simultaneous data loads
    private boolean isLoading = false;

    // Flag to indicate if all data has been loaded
    private boolean isLastPage = false;
    private LinearLayoutManager layoutManager;
    private SearchView searchView;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //init recycler views
        dataRv = view.findViewById(R.id.dataRv);
        progressBar = view.findViewById(R.id.progressBar);
        searchView = view.findViewById(R.id.searchView);


        // get data from firebase database
        reference = FirebaseDatabase.getInstance().getReference().child("Medicine");

        layoutManager = new LinearLayoutManager(getContext());
        dataRv.setLayoutManager(layoutManager);
        dataList = new ArrayList<>();
        filterDataList = new ArrayList<>();
        adapter = new MedicineUserAdapter(filterDataList, getContext()); // change data list, set filterDataList
        dataRv.setAdapter(adapter);

        dataRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= pageSize) {
                        loadData();
                    }
                }

            }
        });

        loadData();

        /*implement search view*/
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                /*create method*/
                performSearch(newText);

                return false;
            }
        });


        return view;
    }

    private void performSearch(String newText) {

        filterData(newText);
        adapter.notifyDataSetChanged();

    }

    private void filterData(String newText) {

        filterDataList.clear();

        if (TextUtils.isEmpty(newText)) {
            filterDataList.addAll(dataList);
        } else {
            for (MedicineData medicineData : dataList) {
                if (medicineData.getBrand().toLowerCase().contains(newText.toLowerCase())) {
                    filterDataList.add(medicineData);
                }
            }
        }

    }


    private void loadData() {

        isLoading = true;

        Query query;

        if (lastKey.isEmpty()) {
            query = reference.orderByKey().limitToFirst(pageSize);
        } else {
            query = reference.orderByKey().startAfter(lastKey).limitToFirst(pageSize);
        }


        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                int count = 0;

                if (dataSnapshot.exists()) {
                    progressBar.setVisibility(View.INVISIBLE);
                    dataRv.setVisibility(View.VISIBLE);

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        MedicineData data = snapshot.getValue(MedicineData.class);
                        dataList.add(data);

                        count++;
                        lastKey = snapshot.getKey();

                    }

                    filterData(null);

                    adapter.notifyDataSetChanged();

                    if (count < pageSize) {
                        isLastPage = true;
                    }

                    isLoading = false;

                } else {

                    progressBar.setVisibility(View.VISIBLE);
                    dataRv.setVisibility(View.GONE);
                    isLoading = false;

                    // offline download data
                    reference.keepSynced(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
            }
        });

    }

}