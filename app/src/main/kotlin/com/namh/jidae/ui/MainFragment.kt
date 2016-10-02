package com.namh.jidae.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.namh.jidae.R


public class MainFragment : Fragment() {



    //------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setRetainInstance(true)

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val fragment = inflater!!.inflate(R.layout.fragment_main, container, false)

//        loadAdView(fragment)


        return fragment
    }

//    fun loadAdView(fragment: View) {
//        val mAdView = fragment.findViewById(R.id.adView) as AdView
//        val adRequest = AdRequest.Builder().build()
//        mAdView.loadAd(adRequest)
//    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

    }






    override fun onStart() {
        super.onStart()

    }


    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }




}