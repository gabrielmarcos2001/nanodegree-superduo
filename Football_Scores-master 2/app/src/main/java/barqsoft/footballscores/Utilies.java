package barqsoft.footballscores;

import android.content.Context;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilies {

    /**
     * Gets the League name from its id
     * @param context
     * @param leagueId
     * @return
     */
    public static String getLeague(Context context, int leagueId) {

        String leagueString = String.valueOf(leagueId);

        switch (leagueString) {
            case AppConfig.SERIE_A:
                return context.getString(R.string.league_serie_a);
            case AppConfig.PREMIER_LEAGUE:
                return context.getString(R.string.league_premiere_league);
            case AppConfig.PRIMERA_DIVISION:
                return context.getString(R.string.league_primera_division);
            case AppConfig.BUNDESLIGA1:
                return context.getString(R.string.league_bundesliga);
            case AppConfig.BUNDESLIGA2:
                return context.getString(R.string.league_bundesliga);
            default:
                return context.getString(R.string.league_unknown);
        }
    }

    /**
     * Returns the Match day formatted text
     * @param context
     * @param match_day
     * @return
     */
    public static String getMatchDay(Context context, int match_day) {
        return context.getString(R.string.matchDay) + " : " + String.valueOf(match_day);
    }

    /**
     * Returns the scores formatted text
     * @param homeGoals
     * @param awayGoals
     * @return
     */
    public static String getScores(int homeGoals, int awayGoals) {
        if (homeGoals < 0 || awayGoals < 0) {
            return " - ";
        } else {
            return String.valueOf(homeGoals) + " - " + String.valueOf(awayGoals);
        }
    }

    /**
     * Returns the team drawable id based on the team id
     * @param teamId
     * @return
     */
    public static int getTeamCrestByTeamId(int teamId) {

        switch (teamId) {

            case AppConfig.TEAM_ARSENAL_ID:
                return R.drawable.arsenal;

            case AppConfig.TEAM_MANCHESTER_UNITED_ID:
                return R.drawable.manchester_united;

            case AppConfig.TEAM_SWANSEA_ID:
                return R.drawable.swansea_city_afc;

            case AppConfig.TEAM_LEICSESTER_ID:
                return R.drawable.leicester_city_fc_hd_logo;

            case AppConfig.TEAM_EVERTON_ID:
                return R.drawable.everton_fc_logo1;

            case AppConfig.TEAM_WEST_HAM_ID:
                return R.drawable.west_ham;

            case AppConfig.TEAM_TOTTEMHAM_ID:
                return R.drawable.tottenham_hotspur;

            case AppConfig.TEAM_WEST_BROMWICH_ID:
                return R.drawable.west_bromwich_albion_hd_logo;

            case AppConfig.TEAM_SUNDERLAND_ID:
                return R.drawable.sunderland;

            case AppConfig.TEAM_STOKE_ID:
                return R.drawable.stoke_city;

            default:
                return R.drawable.ic_launcher;
        }
    }
}
