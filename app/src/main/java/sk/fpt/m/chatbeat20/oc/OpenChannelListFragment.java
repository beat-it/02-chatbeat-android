package sk.fpt.m.chatbeat20.oc;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.sendbird.android.OpenChannel;
import com.sendbird.android.OpenChannelListQuery;
import com.sendbird.android.SendBirdException;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import sk.fpt.m.chatbeat20.MainActivity;
import sk.fpt.m.chatbeat20.R;


public class OpenChannelListFragment extends Fragment {

    public static final String EXTRA_OPEN_CHANNEL_URL = "OPEN_CHANNEL_URL";
    private static final String LOG_TAG = OpenChannelListFragment.class.getSimpleName();

    @BindString(R.string.all_open_channels)
    String mChannelTitle;

    @BindView(R.id.recycler_open_channel_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.swipe_layout_open_channel_list)
    SwipeRefreshLayout mSwipeRefresh;

    private LinearLayoutManager mLayoutManager;
    private OpenChannelListAdapter mChannelListAdapter;
    private OpenChannelListQuery mChannelListQuery;

    public static OpenChannelListFragment newInstance() {
        OpenChannelListFragment fragment = new OpenChannelListFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_open_channel_list, container, false);
        ButterKnife.bind(this, rootView);

        setSwipeLayout();
        setUpTitle();
        setUpCreateChannelButton(rootView);
        setUpAdapter();
        setUpRecyclerView();
        return rootView;
    }

    private void setSwipeLayout() {
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefresh.setRefreshing(true);
                refreshChannelList(15);
            }
        });
    }

    private void setUpTitle() {
        getMainActivity().setActionBarTitle(mChannelTitle);
    }

    private void setUpCreateChannelButton(View rootView) {
        FloatingActionButton mCreateChannelFab = (FloatingActionButton) rootView.findViewById(R.id.fab_open_channel_list);
        mCreateChannelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Enter channel name");
                final EditText edittext = new EditText(getContext());
                alert.setView(edittext);
                alert.setNeutralButton("Create", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createOpenChannel(edittext.getText().toString());
                    }
                });
                alert.show();
            }
        });
    }

    private void createOpenChannel(String name) {
        OpenChannel.createChannelWithOperatorUserIds(name, null, null, null, new OpenChannel.OpenChannelCreateHandler() {
            @Override
            public void onResult(OpenChannel openChannel, SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                }
                refreshChannelList(15);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshChannelList(15);
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mChannelListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mChannelListAdapter.getItemCount() - 1) {
                    loadNextChannelList();
                }
            }
        });
    }

    private void setUpAdapter() {
        mChannelListAdapter = new OpenChannelListAdapter(getContext());
        mChannelListAdapter.setOnItemClickListener(new OpenChannelListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(OpenChannel channel) {
                String channelUrl = channel.getUrl();
                OpenChatFragment fragment = OpenChatFragment.newInstance(channelUrl);
                getFragmentManager().beginTransaction()
                        .replace(R.id.container_open_channel, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        mChannelListAdapter.setOnItemLongClickListener(new OpenChannelListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongPress(OpenChannel channel) {

            }
        });
    }

    void refreshChannelList(int numChannels) {
        mChannelListQuery = OpenChannel.createOpenChannelListQuery();
        mChannelListQuery.setLimit(numChannels);
        mChannelListQuery.next(new OpenChannelListQuery.OpenChannelListQueryResultHandler() {
            @Override
            public void onResult(List<OpenChannel> list, SendBirdException e) {
                if (e == null) {
                    mChannelListAdapter.setOpenChannelList(list);

                    if (mSwipeRefresh.isRefreshing()) {
                        mSwipeRefresh.setRefreshing(false);
                    }
                } else {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                }
            }
        });
    }

    void loadNextChannelList() {
        mChannelListQuery.next(new OpenChannelListQuery.OpenChannelListQueryResultHandler() {
            @Override
            public void onResult(List<OpenChannel> list, SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                    return;
                }

                for (OpenChannel channel : list) {
                    mChannelListAdapter.addLast(channel);
                }
            }
        });
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }


}
