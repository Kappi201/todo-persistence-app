package com.example.todolistapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import database.TodoBaseHelper;
import database.TodoCursorWrapper;
import database.TodoDbSchema;

class TodoModel {

    private static TodoModel sTodoModel;
    private static Context mContext;
//    private ArrayList<Todo> mTodoList;
    private SQLiteDatabase mDatabase;

    static TodoModel get(Context context) {

        mContext = context.getApplicationContext();

        if (sTodoModel == null) {
            sTodoModel = new TodoModel(context);
        }
        return sTodoModel;
    }

    private TodoModel(Context context){

        mContext = context.getApplicationContext();
        mDatabase = new TodoBaseHelper(mContext)
                .getWritableDatabase();

//        mTodoList = new ArrayList<>();

        // refactor to pattern for data plugins
        // simulate some data for testing

// uncomment block for test data into the database
        for (int i=0; i < 30; i++){
            Todo todo = new Todo();
            todo.setTitle("Todo title " + i);
            todo.setDetail("Detail for task " + todo.getId().toString());
            todo.setComplete(0);

            //addTodo(todo);
 //           mTodoList.add(todo);
        }

    }

    void updateTodo(Todo todo){
        String uuidString = todo.getId().toString();
        ContentValues values = getContentValues(todo);

        /* stop sql injection, pass uuidString to new String
        so, it is treated as string rather than code */
        mDatabase.update(TodoDbSchema.TodoTable.NAME, values,
                TodoDbSchema.TodoTable.Cols.UUID + " = ?",
                new String[] { uuidString });
    }

    public void deleteTodo(Todo todo) {
        String uuidString = todo.getId().toString();

        mDatabase.delete(
                TodoDbSchema.TodoTable.NAME,
                TodoDbSchema.TodoTable.Cols.UUID + " = ?",
                new String[] { uuidString });

    }

    private TodoCursorWrapper queryTodoList(String whereClause, String[] whereArgs) {
 //   private Cursor queryTodoList(String whereClause, String[] whereArgs) {
      Cursor cursor = mDatabase.query(
              TodoDbSchema.TodoTable.NAME,
              null, // null for all columns
              whereClause,
              whereArgs,
              null,
              null,
              null
      );

      return new TodoCursorWrapper(cursor);
//      return cursor;
    }

    private static ContentValues getContentValues(Todo todo) {
        ContentValues values = new ContentValues();
        values.put(TodoDbSchema.TodoTable.Cols.UUID, todo.getId().toString());
        values.put(TodoDbSchema.TodoTable.Cols.TITLE, todo.getTitle());
        values.put(TodoDbSchema.TodoTable.Cols.DETAIL, todo.getDetail());
        values.put(TodoDbSchema.TodoTable.Cols.DATE, todo.getDate().getTime());
        values.put(TodoDbSchema.TodoTable.Cols.IS_COMPLETE, todo.isComplete()==1 ? 1 : 0);

        return values;
    }

    List<Todo> getTodoList() {
        /* return mTodoList;
        return new ArrayList<>(); */

        List<Todo> todoList = new ArrayList<>();

        try (TodoCursorWrapper cursor = queryTodoList(null, null)) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                todoList.add(cursor.getTodo());
                cursor.moveToNext();
            }
        }

        return todoList;

    }

    Todo getTodo(UUID id){

        try (TodoCursorWrapper cursor = queryTodoList(
                TodoDbSchema.TodoTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        )) {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getTodo();
        }
    }

    private void addTodo(Todo todo){
//        mTodoList.add(todo);
        ContentValues values = getContentValues(todo);
        mDatabase.insert(TodoDbSchema.TodoTable.NAME, null, values);
    }



    File getPhotoFile(Todo todo){
        File filesDir = mContext.getFilesDir();
        return new File(filesDir, todo.getPhotoFilename());
    }

}