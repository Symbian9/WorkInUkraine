package com.dmelnyk.workinukraine.ui.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.dmelnyk.workinukraine.R;
import com.dmelnyk.workinukraine.data.RequestModel;
import com.dmelnyk.workinukraine.db.di.DbModule;
import com.dmelnyk.workinukraine.services.SearchVacanciesService;
import com.dmelnyk.workinukraine.ui.dialogs.delete.DialogDelete;
import com.dmelnyk.workinukraine.ui.dialogs.downloading.DialogDownloading;
import com.dmelnyk.workinukraine.ui.dialogs.request.DialogRequest;
import com.dmelnyk.workinukraine.ui.search.Contract.ISearchPresenter;
import com.dmelnyk.workinukraine.ui.search.di.DaggerSearchComponent;
import com.dmelnyk.workinukraine.ui.search.di.SearchModule;
import com.dmelnyk.workinukraine.ui.vacancy.VacancyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class SearchFragment extends Fragment implements
        Contract.ISearchView,
        SearchAdapter.AdapterCallback,
        DialogRequest.DialogRequestCallbackListener,
        DialogDelete.DialogDeleteCallbackListener,
        DialogDownloading.DialogDownloadCallbackListener {

    private static final String TAG_DIALOG_DOWNLOADING = "downloading_dialog";
    private static final String TAG_DIALOG_REQUEST = "request_dialog";
    private static final String TAG_DIALOG_DELETE = "delete_dialog";
    private static final int REQUEST_CODE_VACANCY_ACTIVITY = 1001;

    @BindView(R.id.backImageView) ImageView mBackImageView;
    @BindView(R.id.settings_image_view) ImageView mSettingsImageView;
    @BindView((R.id.recyclerView)) RecyclerView mRecyclerView;
    @BindView(R.id.buttonAdd) ImageView mButtonAdd;
    @BindView(R.id.buttonSearch) ImageView mButtonSearch;
    @BindView(R.id.vacancies_count_text_view) TextView mVacanciesCountTextView;
    @BindView(R.id.new_vacancies_text_view) TextView mNewVacanciesTextView;
    @BindView(R.id.refreshed_text_view) TextView mLastUpdateTimeTextView;
    @BindView(R.id.new_text_view) TextView mNewTextView;
    Unbinder unbinder;

    @Inject
    ISearchPresenter presenter;

    private OnFragmentInteractionListener mListener;
    private static String sItemClickedRequest = "";

    private DialogDelete mDialogDelete;
    private DialogDownloading mDialogDownloading;
    private DialogRequest mDialogRequest;
    private ArrayList<RequestModel> mRequestsList;
    private SearchAdapter mAdapter;

    private static int sTotalVacanciesCount = 0;
    private static boolean sDownloadingIsFinished;

    private final BroadcastReceiver mDownloadingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d(" ACTION_CODE = " + intent.getAction());

            String request = intent.getStringExtra(SearchVacanciesService.KEY_REQUEST);
            switch (intent.getAction()) {
                case SearchVacanciesService.ACTION_FINISHED:
                    sTotalVacanciesCount = intent.getIntExtra(SearchVacanciesService.KEY_TOTAL_VACANCIES_COUNT, -1);
                    sDownloadingIsFinished = true;
                    mDialogDownloading.downloadingFinished(sTotalVacanciesCount);
                    presenter.bindView(SearchFragment.this);
                    break;

                case SearchVacanciesService.ACTION_DOWNLOADING_IN_PROGRESS:
//                    sTotalVacanciesCount += intent.getIntExtra(
//                            SearchVacanciesService.KEY_TOTAL_VACANCIES_COUNT, -1);
//                    Toast.makeText(context, request + sTotalVacanciesCount, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerSearchComponent.builder()
                .dbModule(new DbModule(getContext()))
                .searchModule(new SearchModule())
                .build()
                .inject(this);

        mRequestsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        unbinder = ButterKnife.bind(this, view);

        mAdapter = new SearchAdapter(mRequestsList);
        mAdapter.setAdapterListener(this);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        mRecyclerView.setAdapter(mAdapter);

        presenter.bindView(this);

        createMenu();
        return view;
    }

    private void createMenu() {
        final PopupMenu popupMenu = new PopupMenu(getContext(), mSettingsImageView);
        try {
            Field field = popupMenu.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(popupMenu);
            Method setForceIcons = menuPopupHelper.getClass().getDeclaredMethod("setForceShowIcon", Boolean.TYPE);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            Timber.e(e);
        }

        popupMenu.getMenuInflater().inflate(R.menu.search_toolbar, popupMenu.getMenu());
        mSettingsImageView.setOnClickListener(view -> popupMenu.show());

        popupMenu.setOnMenuItemClickListener(view -> {
            switch (view.getItemId()) {
                case R.id.menu_clear_requests:
                    Toast.makeText(getContext(), "Todo: Clearing all requests!", Toast.LENGTH_SHORT).show();
                    presenter.clearAllRequest();
                    break;
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreDialogs();
        // registering downloading receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SearchVacanciesService.ACTION_FINISHED);
        filter.addAction(SearchVacanciesService.ACTION_DOWNLOADING_IN_PROGRESS);
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mDownloadingBroadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(mDownloadingBroadcastReceiver);
    }

    private void restoreDialogs() {
        // restoring DeleteDialog if needed
        mDialogDelete = (DialogDelete) getFragmentManager()
                .findFragmentByTag(TAG_DIALOG_DELETE);
        if (mDialogDelete != null) {
            mDialogDelete.setCallback(this);
        }

        // restoring DownloadingDialog if needed
        mDialogDownloading = (DialogDownloading) getFragmentManager()
                .findFragmentByTag(TAG_DIALOG_DOWNLOADING);
        if (mDialogDownloading != null) {
            if (sDownloadingIsFinished) {
                // removing previous dialog
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.remove(mDialogDownloading);
                mDialogDownloading = DialogDownloading.newInstance(false, sTotalVacanciesCount);
                mDialogDownloading.show(ft, TAG_DIALOG_DOWNLOADING);
                // create new dialog
            }
            mDialogDownloading.setCallback(this);
        }

        // restoring RequestDialog if needed
        mDialogRequest = (DialogRequest) getFragmentManager()
                .findFragmentByTag(TAG_DIALOG_REQUEST);
        if (mDialogRequest != null) {
            mDialogRequest.setCallback(this);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDialogPeriodInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        presenter.unbindView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.backImageView, R.id.buttonAdd, R.id.buttonSearch})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.backImageView:
                mListener.onFragmentInteraction();
                break;
            case R.id.buttonAdd:
                showDialogRequest();
                break;
            case R.id.buttonSearch:
                showDialogDownloading();
                startSearchVacanciesService();
                break;
        }
    }

    private void showDialogRequest() {
        mDialogRequest = DialogRequest.getInstance();
        mDialogRequest.setCallback(this);
        mDialogRequest.show(getFragmentManager(), TAG_DIALOG_REQUEST);
    }

    private void showDialogDownloading() {
        mDialogDownloading = DialogDownloading.newInstance(true, 0);
        mDialogDownloading.setCallback(this);
        mDialogDownloading.show(getFragmentManager(), TAG_DIALOG_DOWNLOADING);
    }

    private void startSearchVacanciesService() {
        Intent searchService = new Intent(
                getContext().getApplicationContext(), SearchVacanciesService.class);
        searchService.putParcelableArrayListExtra(SearchVacanciesService.KEY_REQUESTS, mRequestsList);
        searchService.putExtra(SearchVacanciesService.KEY_MODE, SearchVacanciesService.MODE_SEARCH);
        getContext().startService(searchService);
    }

    @Override
    public void updateData(ArrayList<RequestModel> data) {
        Timber.d(" updateData(ArrayList<RequestModel> data)");
        mRequestsList.clear();
        mRequestsList.addAll(data);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void updateNewVacanciesCount(int newVacanciesCount) {
        mNewVacanciesTextView.setVisibility(
                newVacanciesCount == 0 ? View.GONE : View.VISIBLE);
        mNewTextView.setVisibility(
                newVacanciesCount == 0 ? View.GONE : View.VISIBLE);
        mNewVacanciesTextView.setText("" + newVacanciesCount);
    }

    @Override
    public void updateVacanciesCount(int allVacanciesCount) {
        mVacanciesCountTextView.setText("" + allVacanciesCount);
    }

    @Override
    public void updateLastSearchTime(String updated) {
        mLastUpdateTimeTextView.setText(getString(R.string.refreshed) + ": " + updated);
    }

    @Override
    public void showErrorMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
//        Toast.makeText(getContext(), getString(R.string.errors_db_request_already_exists), Toast.LENGTH_SHORT).show();
    }

    // SearchAdapter.AdapterCallback for open DialogDelete
    @Override
    public void onButtonRemoveClicked(String item) {
        Timber.d("onButtonRemoveClicked on item " + item);
        sItemClickedRequest = item;
        mDialogDelete = DialogDelete.getInstance(getString(R.string.delete_request));
        mDialogDelete.setCallback(this);
        mDialogDelete.show(getFragmentManager(), TAG_DIALOG_DELETE);
    }

    // SearchAdapter.AdapterCallback for open Item Fragment
    @Override
    public void onItemClicked(String itemRequestTitle) {
        Timber.d("SearchAdapter.AdapterCallback.onItemClicked. Item = " + itemRequestTitle);
        sItemClickedRequest = itemRequestTitle;
        Intent vacancyActivityIntent = new Intent(getContext(), VacancyActivity.class);
        vacancyActivityIntent.setAction(itemRequestTitle);
        startActivityForResult(vacancyActivityIntent, REQUEST_CODE_VACANCY_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_VACANCY_ACTIVITY) {
            switch (resultCode) {
                case RESULT_OK:
                    // update request table
                    presenter.getFreshRequests();
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(getContext(), R.string.no_vacancies_found, Toast.LENGTH_LONG)
                            .show();
                    break;
            }
        }
    }

    // DialogRequestCallbackListener add item
    @Override
    public void onTakeRequest(String request) {
        presenter.addNewRequest(request);
    }

    // DialogRequestCallbackListener remove item
    @Override
    public void onRemoveRequest() {
        Timber.d("onRemoveRequest clicked. Item = " + sItemClickedRequest);
        presenter.removeRequest(sItemClickedRequest);
    }

    // DialogDownloadCallbackListener dismiss dialog
    @Override
    public void onDismissDialogDownloading() {
        // TODO: remove this method
//        sIsDialogDownloadingOpen = false;
        sTotalVacanciesCount = 0;
    }

    public interface OnFragmentInteractionListener {
        // for open NavigationDrawer
        void onFragmentInteraction();
    }
}
