/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namh.jidae;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class PodMainFragment extends Fragment {

    final Handler handler = new Handler();


    ViewPager pager;
    FragmentPagerAdapter adapter;


    Drawable oldBackground = null;
    int currentColor = 0xFF2d3586;
    String mCurrentYoutubeId;
    int mYoutubeState;
    int mCurrentPage;

    int mCurrentPagerItemIndex = 0;



    //------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);






    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragment = inflater.inflate(R.layout.fragment_main, container, false);

        // ad view
        loadAdView(fragment);

        RecyclerView rv = (RecyclerView) fragment.findViewById(R.id.rv_podlist);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        rv.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragment.getContext());
        rv.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        String[] myDataset = {"Test1", "Test2", "Test3"};
        PodListAdapter mAdapter = new PodListAdapter(myDataset);
        rv.setAdapter(mAdapter);


//
//        rv.addOnItemTouchListener(
//                new RecyclerItemClickListener(fragment.getContext(),
//                        new RecyclerItemClickListener.OnItemClickListener() {
//
//                            @Override
//                            public void onItemClick(View view, int position) {
//                                // TODO Handle item click
//                                Log.d("tttt", Integer.toString(position));
//                                // show youtube
//                                YouTubeVideoItem item = twoColsTubeItemAdapter.getItem(position);
//                                YouTubeUtils.startYoutubeActivity(getActivity(), item);
//                            }
//                        })
//        );


        return fragment;
    }

    void loadAdView(View fragment) {
        AdView mAdView = (AdView) fragment.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setUpPager(view);
        setUpTabColor();
    }

    void setUpPager(View view){




//        REMOVE_PAGERSLIDINGTABSTRIP
//        tabs = (PagerSlidingTabStrip) getView().findViewById(R.id.tabs);
//        tabs.setViewPager(pager);
    }




    void setUpTabColor(){

    }

    @Override
    public void onStart() {
        super.onStart();


        // tabs --> pager --> adapter

//        pager = (ViewPager) getView().findViewById(R.id.pager);
//        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
//                getResources().getDisplayMetrics());
//        pager.setPageMargin(pageMargin);
//        adapter = new CocubePagerAdapter(getView().getContext(), getChildFragmentManager());
//
//        pager.setAdapter(adapter);
//        pager.setCurrentItem(mCurrentPagerItemIndex);
//
//        mSlidingTabLayout = (SlidingTabLayout) getView().findViewById(R.id.sliding_tabs);
//        mSlidingTabLayout.setViewPager(pager);

//        tabs = (PagerSlidingTabStrip) getView().findViewById(R.id.tabs);
//        tabs.setViewPager(pager);

    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

        mCurrentPagerItemIndex = pager.getCurrentItem();

    }


    public void notifyDataSetChanged() {
        if(adapter != null)
            adapter.notifyDataSetChanged();
    }



}