package sk.fpt.m.chatbeat20.oc;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import sk.fpt.m.chatbeat20.MainActivity;
import sk.fpt.m.chatbeat20.R;
import sk.fpt.m.chatbeat20.util.FileUtils;
import sk.fpt.m.chatbeat20.util.PhotoViewerActivity;


public class OpenChatFragment extends Fragment {

    private static final String LOG_TAG = OpenChatFragment.class.getSimpleName();

    private static final String CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_OPEN_CHAT";
    private static final String CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_OPEN_CHAT";

    private static final int INTENT_REQUEST_CHOOSE_IMAGE = 300;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 13;

    private RecyclerView mRecyclerView;
    private OpenChatAdapter mChatAdapter;
    private LinearLayoutManager mLayoutManager;
    private View mRootLayout;
    private EditText mMessageEditText;

    private OpenChannel mChannel;
    private String mChannelUrl;
    private PreviousMessageListQuery mPrevMessageListQuery;

    @BindView(R.id.button_open_channel_chat_send)
    Button mMessageSendButton;

    @BindView(R.id.button_open_channel_pacik_send)
    ImageButton mPacikSendButton;

    public static OpenChatFragment newInstance(@NonNull String channelUrl) {
        Bundle args = new Bundle();
        args.putString(OpenChannelListFragment.EXTRA_OPEN_CHANNEL_URL, channelUrl);
        OpenChatFragment fragment = new OpenChatFragment();
        fragment.setRetainInstance(true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_open_chat, container, false);
        setHasOptionsMenu(true);
        ButterKnife.bind(this, rootView);

        mRootLayout = rootView.findViewById(R.id.layout_open_chat_root);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_open_channel_chat);

        setUpChatAdapter();
        setUpRecyclerView();

        mMessageEditText = (EditText) rootView.findViewById(R.id.edittext_chat_message);
        mMessageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(EditText.class.cast(v).getText().toString().isEmpty()) {
                    mPacikSendButton.setVisibility(View.VISIBLE);
                    mMessageSendButton.setVisibility(View.GONE);

                } else {
                    mPacikSendButton.setVisibility(View.GONE);
                    mMessageSendButton.setVisibility(View.VISIBLE);
                }
                return false;
            }

        });

        mMessageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserMessage(mMessageEditText.getText().toString());
                mMessageEditText.setText("");
            }
        });
        mPacikSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dajPacik();
            }
        });


        ImageButton mUploadFileButton = (ImageButton) rootView.findViewById(R.id.button_open_channel_chat_upload);
        mUploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestImage();
            }
        });
        mChannelUrl = getArguments().getString(OpenChannelListFragment.EXTRA_OPEN_CHANNEL_URL);
        enterChannel(mChannelUrl);
        return rootView;
    }

    private String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    private void dajPacik() {
        int pacik = 0x1F44D;
        sendUserMessage(getEmojiByUnicode(pacik));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission granted.
                    Snackbar.make(mRootLayout, "Storage permissions granted. You can now upload or download files.",
                            Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    // Permission denied.
                    Snackbar.make(mRootLayout, "Permissions denied.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Log.d(LOG_TAG, "data is null!");
                return;
            }
            showUploadConfirmDialog(data.getData());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SendBird.setAutoBackgroundDetection(true);
        SendBird.addConnectionHandler(CONNECTION_HANDLER_ID, new SendBird.ConnectionHandler() {
            @Override
            public void onReconnectStarted() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectStarted()");
            }

            @Override
            public void onReconnectSucceeded() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectSucceeded()");
            }

            @Override
            public void onReconnectFailed() {
                Log.d("CONNECTION", "OpenChatFragment onReconnectFailed()");
            }
        });

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (baseChannel.getUrl().equals(mChannelUrl)) {
                    mChatAdapter.addFirst(baseMessage);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mChannel.exit(new OpenChannel.OpenChannelExitHandler() {
            @Override
            public void onResult(SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(),e.getMessage());
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID);
    }

    private void setUpChatAdapter() {
        mChatAdapter = new OpenChatAdapter(getActivity());
        mChatAdapter.setOnItemClickListener(new OpenChatAdapter.OnItemClickListener() {
            @Override
            public void onUserMessageItemClick(UserMessage message) {
            }

            @Override
            public void onFileMessageItemClick(FileMessage message) {
                onFileMessageClicked(message);
            }
        });

        mChatAdapter.setOnItemLongClickListener(new OpenChatAdapter.OnItemLongClickListener() {
            @Override
            public void onBaseMessageLongClick(final BaseMessage message) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Delete message?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteMessage(message);
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setReverseLayout(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mChatAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mChatAdapter.getItemCount() - 1) {
                    loadNextMessageList(30);
                }
                Log.v(LOG_TAG, "onScrollStateChanged");
            }
        });
    }

    private void onFileMessageClicked(FileMessage message) {
        String type = message.getType().toLowerCase();
        if (type.startsWith("image")) {
            Intent i = new Intent(getActivity(), PhotoViewerActivity.class);
            i.putExtra("url", message.getUrl());
            i.putExtra("type", message.getType());
            startActivity(i);
        } else {
            showDownloadConfirmDialog(message);
        }
    }

    private void showDownloadConfirmDialog(final FileMessage message) {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage("Download file?")
                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                FileUtils.downloadFile(getActivity(), message.getUrl(), message.getName());
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null).show();
        }

    }

    private void showUploadConfirmDialog(final Uri uri) {
        new AlertDialog.Builder(getActivity())
                .setMessage("Upload file?")
                .setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {

                            List<FileMessage.ThumbnailSize> thumbnailSizes = new ArrayList<>();
                            thumbnailSizes.add(new FileMessage.ThumbnailSize(240, 240));
                            thumbnailSizes.add(new FileMessage.ThumbnailSize(320, 320));

                            sendImageWithThumbnail(uri, thumbnailSizes);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void requestImage() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions();
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), INTENT_REQUEST_CHOOSE_IMAGE);
            SendBird.setAutoBackgroundDetection(false);
        }
    }

    private void requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mRootLayout, "Storage access permissions are required to upload/download files.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Okay", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSION_WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .show();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void enterChannel(String channelUrl) {
        OpenChannel.getChannel(channelUrl, new OpenChannel.OpenChannelGetHandler() {
            @Override
            public void onResult(final OpenChannel openChannel, SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                    return;
                }

                openChannel.enter(new OpenChannel.OpenChannelEnterHandler() {
                    @Override
                    public void onResult(SendBirdException e) {
                        if (e != null) {
                            getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                            return;
                        }

                        mChannel = openChannel;
                        loadInitialMessageList(30);
                        ((MainActivity) getActivity()).setActionBarTitle(mChannel.getName());
                    }
                });
            }
        });
    }

    private void sendUserMessage(String text) {
        mChannel.sendUserMessage(text, new BaseChannel.SendUserMessageHandler() {
            @Override
            public void onSent(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    Log.e(LOG_TAG, e.toString());
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                    return;
                }
                mChatAdapter.addFirst(userMessage);
            }
        });
    }

    private void sendImageWithThumbnail(Uri uri, List<FileMessage.ThumbnailSize> thumbnailSizes) {
        Hashtable<String, Object> info = FileUtils.getFileInfo(getActivity(), uri);
        final String path = (String) info.get("path");
        final File file = new File(path);
        final String name = file.getName();
        final String mime = (String) info.get("mime");
        final int size = (Integer) info.get("size");

        if (path.equals("")) {
            Toast.makeText(getActivity(), "File must be located in local storage.", Toast.LENGTH_LONG).show();
        } else {
            mChannel.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, new BaseChannel.SendFileMessageHandler() {
                @Override
                public void onSent(FileMessage fileMessage, SendBirdException e) {
                    if (e != null) {
                        getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                        return;
                    }
                    mChatAdapter.addFirst(fileMessage);
                }
            });
        }
    }

    private void loadInitialMessageList(int numMessages) {

        mPrevMessageListQuery = mChannel.createPreviousMessageListQuery();
        mPrevMessageListQuery.load(numMessages, true, new PreviousMessageListQuery.MessageListQueryResult() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (e != null) {
                    // Error!
                    e.printStackTrace();
                    return;
                }

                mChatAdapter.setMessageList(list);
            }
        });

    }

    private void loadNextMessageList(int numMessages) throws NullPointerException {

        if (mChannel == null) {
            throw new NullPointerException("Current channel instance is null.");
        }

        if (mPrevMessageListQuery == null) {
            throw new NullPointerException("Current query instance is null.");
        }


        mPrevMessageListQuery.load(numMessages, true, new PreviousMessageListQuery.MessageListQueryResult() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                    return;
                }
                for (BaseMessage message : list) {
                    mChatAdapter.addLast((message));
                }
            }
        });
    }

    private void deleteMessage(final BaseMessage message) {
        mChannel.deleteMessage(message, new BaseChannel.DeleteMessageHandler() {
            @Override
            public void onResult(SendBirdException e) {
                if (e != null) {
                    getMainActivity().showErrorToast(e.getCode(), e.getMessage());
                    return;
                }
                loadInitialMessageList(30);
            }
        });
    }


    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}
