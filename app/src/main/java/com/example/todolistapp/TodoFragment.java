package com.example.todolistapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class TodoFragment extends Fragment {

    private static final String ARG_TODO_ID = "todo_id";
    private static final int REQUEST_PHOTO = 2;

    private Todo mTodo;
    private EditText mEditTextTitle;
    private Button mButtonDate;
    private Button mButtonDelete;
    private CheckBox mCheckBoxIsComplete;

    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private File mPhotoFile;

    /*
    Rather than the calling the constructor directly, Activity(s) should call newInstance
    and pass required parameters that the fragment needs to create its arguments.
     */
    public static TodoFragment newInstance(UUID todoId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TODO_ID, todoId);

        TodoFragment fragment = new TodoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        /*
         Fragment accessing the intent from the hosting Activity as in the following code snippet
         allows for simple code that works.

        UUID todoId = (UUID) getActivity()
                .getIntent().getSerializableExtra(TodoActivity.EXTRA_TODO_ID);

         The disadvantage: TodoFragment is no longer reusable as it is coupled to Activities whoes
         intent has to contain the todoId.

         Solution: store the todoId in the fragment's arguments bundle.
            See the TodoFragment newInstance(UUID todoId) method.

         Then to create a new fragment, the TodoActivity should call TodoFragment.newInstance(UUID)
         and pass in the UUID it retrieves from its extra argument.

        */

        UUID todoId = (UUID) getArguments().getSerializable(ARG_TODO_ID);
        mTodo = TodoModel.get(getActivity()).getTodo(todoId);
        mPhotoFile = TodoModel.get(getActivity()).getPhotoFile(mTodo);

    }

    @Override
    public void onPause() {
        super.onPause();
        TodoModel.get(getActivity()).updateTodo(mTodo);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        mEditTextTitle = (EditText) view.findViewById(R.id.todo_title);
        mEditTextTitle.setText(mTodo.getTitle());
        mEditTextTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This line is intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTodo.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This line is intentionally left blank
            }
        });

        mButtonDate = (Button) view.findViewById(R.id.todo_date);
        mButtonDate.setText(mTodo.getDate().toString());
        mButtonDate.setEnabled(false);

        mButtonDelete = (Button) view.findViewById(R.id.todo_delete);

        mCheckBoxIsComplete = (CheckBox) view.findViewById(R.id.todo_complete);
        mCheckBoxIsComplete.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("DEBUG **** TodoFragment", "called onCheckedChanged");
                mTodo.setComplete(isChecked==true ? 1 : 0);
            }
        });

        mPhotoButton = (ImageButton) view.findViewById(R.id.todo_camera);
        mPhotoView = (ImageView) view.findViewById(R.id.todo_photo);

        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        PackageManager packageManager = getActivity().getPackageManager();
        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri uri = FileProvider.getUriForFile(getActivity(),
                        "com.example.todolistapp.fileprovider",
                        mPhotoFile);

                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                List<ResolveInfo> cameraActivities = getActivity()
                        .getPackageManager().queryIntentActivities(captureImage,
                                PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo activity : cameraActivities) {
                    getActivity().grantUriPermission(activity.activityInfo.packageName,
                            uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                startActivityForResult(captureImage, REQUEST_PHOTO);

            }
        });

        mButtonDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                /*Toast.makeText(
                        getActivity(),
                        mTodo.getTitle() + " clicked",
                        Toast.LENGTH_SHORT)
                        .show();*/
                Intent intent = new Intent(v.getContext(), TodoListActivity.class);
                //mTodo.setTitle("it worked");
                TodoModel.get(getActivity()).deleteTodo(mTodo);
                startActivityForResult(intent, 0);
            }
        });


        mPhotoView = (ImageView) view.findViewById(R.id.todo_photo);
        updatePhotoView();

        return view;

    }

    private void updatePhotoView(){
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode != Activity.RESULT_OK){
            return;
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.example.todolistapp.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            updatePhotoView();
        }
    }

}

