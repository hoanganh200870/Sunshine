package hcmut.hoanganh.sunshine.data;// /*
//  * Copyright (C) 2014 The Android Open Source Project
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// import android.content.ContentProvider;
// import android.content.ContentUris;
// import android.content.ContentValues;
// import android.content.UriMatcher;
// import android.database.Cursor;
// import android.database.sqlite.SQLiteDatabase;
// import android.database.sqlite.SQLiteQueryBuilder;
// import android.net.Uri;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WeatherProvider extends ContentProvider {

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);
        uriMatcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        uriMatcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return uriMatcher;
    }

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mOpenHelper = new WeatherDbHelper(context);
        return true;
    }

    private static final SQLiteQueryBuilder sWeatherByLocationSetting;

    static {
        sWeatherByLocationSetting = new SQLiteQueryBuilder();
        sWeatherByLocationSetting.setTables(WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN "
                + WeatherContract.LocationEntry.TABLE_NAME + " ON "
                + WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = "
                + WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry._ID);
    }

    private static final String sLocationSettingSelection = WeatherContract.LocationEntry.TABLE_NAME + "."
            + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    private static final String sLocationSettingWithStartDateSelection = WeatherContract.LocationEntry.TABLE_NAME + "."
            + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND "
            + WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String sLocationSettingWithDateSelection = WeatherContract.LocationEntry.TABLE_NAME + "."
            + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND "
            + WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String selection;
        String[] selectionArgs;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[] { locationSetting };
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[] { locationSetting, startDate };
        }

        SQLiteDatabase reader = mOpenHelper.getReadableDatabase();
        return sWeatherByLocationSetting.query(reader, projection, selection, selectionArgs, null, null, sortOder);
    }

    private Cursor getWeatherByLocationWithDateSetting(Uri uri, String[] projection, String sortOder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        String selection = sLocationSettingWithDateSelection;
        String[] selectionArgs = new String[] { locationSetting, date };

        SQLiteDatabase reader = mOpenHelper.getReadableDatabase();
        return sWeatherByLocationSetting.query(reader, projection, selection, selectionArgs, null, null, sortOder);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        Cursor cursor = null;

        int type = sUriMatcher.match(uri);
        switch (type) {
            case WEATHER: // "weather"
                cursor = database.query(WeatherContract.WeatherEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case WEATHER_WITH_LOCATION: // "weather/*"
                cursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            case WEATHER_WITH_LOCATION_AND_DATE: // "weather/*/*"
                cursor = getWeatherByLocationWithDateSetting(uri, projection, sortOrder);
                break;
            case LOCATION: // "location"
                cursor = database.query(WeatherContract.LocationEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case LOCATION_ID: // "location/#"
                long id = ContentUris.parseId(uri);
                String[] args = { String.valueOf(id) };
                String select = WeatherContract.LocationEntry._ID + " = ? ";
                cursor = database.query(WeatherContract.LocationEntry.TABLE_NAME, projection, select, args, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        ContentResolver contentResolver = getContext().getContentResolver();
        cursor.setNotificationUri(contentResolver, uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase writer = mOpenHelper.getWritableDatabase();
        Uri returnUri;

        int type = sUriMatcher.match(uri);
        switch (type) {
            case WEATHER:
                long weatherId = writer.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if (weatherId != -1) {
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(weatherId);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            case LOCATION:
                long locationId = writer.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if (locationId != -1) {
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(locationId);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        ContentResolver contentResolver = getContext().getContentResolver();
        contentResolver.notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase writer = mOpenHelper.getWritableDatabase();

        int rowIdDeleted;
        int type = sUriMatcher.match(uri);
        switch (type) {
            case WEATHER:
                rowIdDeleted = writer.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowIdDeleted = writer.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (selection == null || rowIdDeleted != 0) {
            ContentResolver contentResolver = getContext().getContentResolver();
            contentResolver.notifyChange(uri, null);
        }

        return rowIdDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        SQLiteDatabase writer = mOpenHelper.getWritableDatabase();

        int rowsUpdated;
        int type = sUriMatcher.match(uri);
        switch (type) {
            case WEATHER:
                rowsUpdated = writer.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated = writer.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            ContentResolver contentResolver = getContext().getContentResolver();
            contentResolver.notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                ContentResolver contentResolver = getContext().getContentResolver();
                contentResolver.notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}

//     // The URI Matcher used by this content provider.
//     private static final UriMatcher sUriMatcher = buildUriMatcher();
//     private WeatherDbHelper mOpenHelper;

//     private static final int WEATHER = 100;
//     private static final int WEATHER_WITH_LOCATION = 101;
//     private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
//     private static final int LOCATION = 300;
//     private static final int LOCATION_ID = 301;

//     private static UriMatcher buildUriMatcher() {
//         // I know what you're thinking.  Why create a UriMatcher when you can use regular
//         // expressions instead?  Because you're not crazy, that's why.

//         // All paths added to the UriMatcher have a corresponding code to return when a match is
//         // found.  The code passed into the constructor represents the code to return for the root
//         // URI.  It's common to use NO_MATCH as the code for this case.
//         final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
//         final String authority = WeatherContract.CONTENT_AUTHORITY;

//         // For each type of URI you want to add, create a corresponding code.
//         matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
//         matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
//         matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

//         matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
//         matcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

//         return matcher;
//     }

//     private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

//     static{
//         sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
//         sWeatherByLocationSettingQueryBuilder.setTables(
//                 WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
//                         WeatherContract.LocationEntry.TABLE_NAME +
//                         " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
//                         "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
//                         " = " + WeatherContract.LocationEntry.TABLE_NAME +
//                         "." + WeatherContract.LocationEntry._ID);
//     }

//     private static final String sLocationSettingSelection =
//             WeatherContract.LocationEntry.TABLE_NAME+
//                     "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
//     private static final String sLocationSettingWithStartDateSelection =
//             WeatherContract.LocationEntry.TABLE_NAME+
//                     "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
//                     WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";

//     private static final String sLocationSettingAndDaySelection =
//             WeatherContract.LocationEntry.TABLE_NAME +
//                     "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
//                     WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

//     private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
//         String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
//         String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

//         String[] selectionArgs;
//         String selection;

//         if (startDate == null) {
//             selection = sLocationSettingSelection;
//             selectionArgs = new String[]{locationSetting};
//         } else {
//             selectionArgs = new String[]{locationSetting, startDate};
//             selection = sLocationSettingWithStartDateSelection;
//         }

//         return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
//                 projection,
//                 selection,
//                 selectionArgs,
//                 null,
//                 null,
//                 sortOrder
//         );
//     }

//     private Cursor getWeatherByLocationSettingAndDate(
//             Uri uri, String[] projection, String sortOrder) {
//         String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
//         String date = WeatherContract.WeatherEntry.getDateFromUri(uri);

//         return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
//                 projection,
//                 sLocationSettingAndDaySelection,
//                 new String[]{locationSetting, date},
//                 null,
//                 null,
//                 sortOrder
//         );
//     }

//     @Override
//     public boolean onCreate() {
//         mOpenHelper = new WeatherDbHelper(getContext());
//         return true;
//     }

//     @Override
//     public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
//                         String sortOrder) {
//         // Here's the switch statement that, given a URI, will determine what kind of request it is,
//         // and query the database accordingly.
//         Cursor retCursor;
//         switch (sUriMatcher.match(uri)) {
//             // "weather/*/*"
//             case WEATHER_WITH_LOCATION_AND_DATE:
//             {
//                 retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
//                 break;
//             }
//             // "weather/*"
//             case WEATHER_WITH_LOCATION: {
//                 retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
//                 break;
//             }
//             // "weather"
//             case WEATHER: {
//                 retCursor = mOpenHelper.getReadableDatabase().query(
//                         WeatherContract.WeatherEntry.TABLE_NAME,
//                         projection,
//                         selection,
//                         selectionArgs,
//                         null,
//                         null,
//                         sortOrder
//                 );
//                 break;
//             }

//             /**
//              * TODO YOUR CODE BELOW HERE FOR QUIZ
//              * QUIZ - 4b - Implement Location_ID queries
//              * https://www.udacity.com/course/viewer#!/c-ud853/l-1576308909/e-1675098551/m-1675098552
//              **/

//             default:
//                 throw new UnsupportedOperationException("Unknown uri: " + uri);
//         }
//         retCursor.setNotificationUri(getContext().getContentResolver(), uri);
//         return retCursor;
//     }

//     @Override
//     public String getType(Uri uri) {

//         // Use the Uri Matcher to determine what kind of URI this is.
//         final int match = sUriMatcher.match(uri);

//         switch (match) {
//             case WEATHER_WITH_LOCATION_AND_DATE:
//                 return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
//             case WEATHER_WITH_LOCATION:
//                 return WeatherContract.WeatherEntry.CONTENT_TYPE;
//             case WEATHER:
//                 return WeatherContract.WeatherEntry.CONTENT_TYPE;

//             /**
//              * TODO YOUR CODE BELOW HERE FOR QUIZ
//              * QUIZ - 4b - Coding the Content Provider : getType
//              * https://www.udacity.com/course/viewer#!/c-ud853/l-1576308909/e-1675098546/m-1675098547
//              **/

//             default:
//                 throw new UnsupportedOperationException("Unknown uri: " + uri);
//         }
//     }

//     @Override
//     public Uri insert(Uri uri, ContentValues values) {
//         final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//         final int match = sUriMatcher.match(uri);
//         Uri returnUri;

//         switch (match) {
//             case WEATHER: {
//                 long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
//                 if ( _id > 0 )
//                     returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
//                 else
//                     throw new android.database.SQLException("Failed to insert row into " + uri);
//                 break;
//             }
//             case LOCATION: {
//                 long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
//                 if ( _id > 0 )
//                     returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
//                 else
//                     throw new android.database.SQLException("Failed to insert row into " + uri);
//                 break;
//             }
//             default:
//                 throw new UnsupportedOperationException("Unknown uri: " + uri);
//         }
//         getContext().getContentResolver().notifyChange(uri, null);
//         return returnUri;
//     }

//     @Override
//     public int delete(Uri uri, String selection, String[] selectionArgs) {
//         final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//         final int match = sUriMatcher.match(uri);
//         int rowsDeleted;
//         switch (match) {
//             case WEATHER:
//                 rowsDeleted = db.delete(
//                         WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
//                 break;
//             case LOCATION:
//                 rowsDeleted = db.delete(
//                         WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
//                 break;
//             default:
//                 throw new UnsupportedOperationException("Unknown uri: " + uri);
//         }
//         // Because a null deletes all rows
//         if (selection == null || rowsDeleted != 0) {
//             getContext().getContentResolver().notifyChange(uri, null);
//         }
//         return rowsDeleted;
//     }

//     @Override
//     public int update(
//             Uri uri, ContentValues values, String selection, String[] selectionArgs) {
//         /**
//          * TODO YOUR CODE BELOW HERE FOR QUIZ
//          * QUIZ - 4b - Updating and Deleting
//          * https://www.udacity.com/course/viewer#!/c-ud853/l-1576308909/e-1675098563/m-1675098564
//          **/
//         return 0;
//     }

//     @Override
//     public int bulkInsert(Uri uri, ContentValues[] values) {
//         final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
//         final int match = sUriMatcher.match(uri);
//         switch (match) {
//             case WEATHER:
//                 db.beginTransaction();
//                 int returnCount = 0;
//                 try {
//                     for (ContentValues value : values) {
//                         long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
//                         if (_id != -1) {
//                             returnCount++;
//                         }
//                     }
//                     db.setTransactionSuccessful();
//                 } finally {
//                     db.endTransaction();
//                 }
//                 getContext().getContentResolver().notifyChange(uri, null);
//                 return returnCount;
//             default:
//                 return super.bulkInsert(uri, values);
//         }
//     }
// }
