package barqsoft.footballscores.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by gabrielmarcos on 10/3/15.
 */
public class Match implements Parcelable {

    public String homeName;
    public String awayName;
    public String date;
    public int homeGoals;
    public int awayGoals;
    public String matchId;

    public Match() {

    }

    /**
     * Parcel Constructor
     * @param in
     */
    public Match(Parcel in){
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.homeName);
        dest.writeString(this.awayName);
        dest.writeString(this.date);
        dest.writeInt(this.homeGoals);
        dest.writeInt(this.awayGoals);
        dest.writeString(this.matchId);
    }

    private void readFromParcel(Parcel in) {
        this.homeName = in.readString();
        this.awayName = in.readString();
        this.date = in.readString();
        this.homeGoals = in.readInt();
        this.awayGoals = in.readInt();
        this.matchId = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Match createFromParcel(Parcel in) {
            return new Match(in);
        }

        public Match[] newArray(int size) {
            return new Match[size];
        }
    };
}
