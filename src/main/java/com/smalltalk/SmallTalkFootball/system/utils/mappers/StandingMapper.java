package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.WinLossDraw;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;

@Component
@Qualifier("standingMapper")
public class StandingMapper implements Mapper<StandingsDtoItem, Standing> {

    @Override
    public Standing map(@NotNull StandingsDtoItem standingsDtoItem) {
        return Standing.builder()
                .competition(Competition.fromCode(Integer.parseInt(standingsDtoItem.getLeagueId())))
                .position(mapPosition(standingsDtoItem))
                .playedMatches(mapPlayedMatches(standingsDtoItem))
                .points(calculatePoints(standingsDtoItem.getOverallLeagueW(), standingsDtoItem.getOverallLeagueD()))
                .overall(mapWinLossDraw(
                        standingsDtoItem.getOverallLeagueW(),
                        standingsDtoItem.getOverallLeagueL(),
                        standingsDtoItem.getOverallLeagueD())
                )
                .home(mapWinLossDraw(
                        standingsDtoItem.getHomeLeagueW(),
                        standingsDtoItem.getHomeLeagueL(),
                        standingsDtoItem.getHomeLeagueD())
                )
                .away(mapWinLossDraw(
                        standingsDtoItem.getAwayLeagueW(),
                        standingsDtoItem.getAwayLeagueL(),
                        standingsDtoItem.getAwayLeagueD()))

                .build();
    }

    private Integer calculatePoints(String w, String d) {
        if (!StringUtils.hasText(w) || !StringUtils.hasText(d)) {
            return null;
        }

        int wins = Integer.parseInt(w);
        int draws = Integer.parseInt(d);

        return wins * 3 + draws;
    }

    private Integer mapPlayedMatches(StandingsDtoItem standingsDtoItem) {
        String playedMatches = standingsDtoItem.getOverallLeaguePayed();
        if (!StringUtils.hasText(playedMatches)) {
            return null;
        }
        return Integer.parseInt(playedMatches);
    }

    private WinLossDraw mapWinLossDraw(String leagueW, String leagueL, String leagueD) {
        Integer win = StringUtils.hasText(leagueW) ? Integer.parseInt(leagueW) : null;
        Integer loss = StringUtils.hasText(leagueL) ? Integer.parseInt(leagueL) : null;
        Integer draw = StringUtils.hasText(leagueD) ? Integer.parseInt(leagueD) : null;

        return new WinLossDraw(win, loss, draw);
    }

    private Integer mapPosition(StandingsDtoItem standingsDtoItem) {
        String position = standingsDtoItem.getOverallLeaguePosition();
        if (!StringUtils.hasText(position)) {
            return null;
        }
        return Integer.parseInt(position);
    }

}
