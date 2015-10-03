package it.jaschke.alexandria.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gabrielmarcos on 8/31/15.
 */
public class Book implements Parcelable {

    public String ean;
    public String title;
    public String subTitle;
    public String description;
    public String imageUrl;
    public List<String> authors = new ArrayList<>();
    public List<String> categories = new ArrayList<>();

    public Book() {
        ean = "";
        title = "";
        subTitle = "";
        imageUrl = "";
        authors = new ArrayList<>();
        categories = new ArrayList<>();
    }

    /**
     * Parcel Constructor
     * @param in
     */
    public Book(Parcel in){
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(this.ean);
        dest.writeString(this.title);
        dest.writeString(this.subTitle);
        dest.writeString(this.description);
        dest.writeString(this.imageUrl);
        dest.writeStringList(this.authors);
        dest.writeStringList(this.categories);
    }

    /**
     * Reads the address data from a parcel object
     * @param in
     */
    private void readFromParcel(Parcel in) {

        this.ean = in.readString();
        this.title = in.readString();
        this.subTitle = in.readString();
        this.description = in.readString();
        this.imageUrl = in.readString();
        in.readStringList(authors);
        in.readStringList(categories);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };
}
