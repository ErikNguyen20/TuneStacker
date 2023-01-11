package com.example.cloudplaylistmanager.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.databinding.FragmentDashboardBinding;
import com.google.android.material.tabs.TabLayout;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private FragmentDashboardBinding binding;

    private ViewPager2 viewPager2;
    private TabLayout tabLayout;
    private DashboardViewPagerAdapter viewPagerAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        this.binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = this.binding.getRoot();


        //Binds the children fragments onto the viewpager & adapter.
        this.viewPager2 = binding.viewPager;
        this.tabLayout = binding.tabLayout;
        this.viewPagerAdapter = new DashboardViewPagerAdapter(getChildFragmentManager(), getLifecycle());
        this.viewPager2.setAdapter(this.viewPagerAdapter);

        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Induce an update.
        this.dashboardViewModel.updateData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}