package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Created by W10 on 9/16/2017.
 */

public class PetProvider extends ContentProvider {

    /*Tag for the log messages*/
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    /*Uri matcher code for complete pets table*/
    private static final int PETS = 100;

    /*Uri matcher code for particular row id*/
    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /*database helper object*/
    private PetDbHelper mDbHelper;

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        Cursor cursor;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        int match = sUriMatcher.match(uri);

        switch (match){
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null,sortOrder);
                break;

            case PETS:
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new  IllegalArgumentException("Cannot query unknown URI" + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override // for mime type information
    public String getType(@NonNull Uri uri) {
        int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        /*
        * This method returns the number of rows deleted from the database
        * */
        int match = sUriMatcher.match(uri);
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        switch (match){
            case PETS:
                return deletePet(uri, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return deletePet(uri, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Cannot delete for URI " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {

        int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Cannot update for URI " + uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues values){

        String currentPetName = values.getAsString(PetEntry.COLUMN_PET_NAME);
        Integer currentPetGender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        Integer currentPetWeight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);

        if (currentPetName == null) {
            throw new IllegalArgumentException("Pet requires a name");
        } else if (currentPetWeight == null || currentPetWeight < 0){
            throw new IllegalArgumentException("Pet requires valid weight");
        }else if (currentPetGender == null || !PetEntry.isValidGender(currentPetGender)){
            throw new IllegalArgumentException("Pet requires valid gender");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long newRowId = database.insert(PetContract.PetEntry.TABLE_NAME, null, values);

        if (newRowId == -1){
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, newRowId);
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArg){

        if (values.containsKey(PetEntry.COLUMN_PET_NAME)){
            String updatedPetName = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (updatedPetName == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        } else if(values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            Integer updatedPetWeight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (updatedPetWeight == null || updatedPetWeight <=0) {
            throw new IllegalArgumentException("Pet requires valid weight");
            }
        } else if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer updatedPetGender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (updatedPetGender == null || !PetEntry.isValidGender(updatedPetGender)){
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        } else if (values.size() == 0){
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated =  database.update(PetEntry.TABLE_NAME, values, selection, selectionArg);

        if (rowsUpdated != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    private int deletePet(Uri uri, String selection, String[] selectionArg){

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArg);

        if (rowsDeleted != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

}
