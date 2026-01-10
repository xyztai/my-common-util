package net.my.mapper;

import net.my.pojo.EastmoneyWinRatioPOJO;
import net.my.pojo.SpecialCarePoJo2;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgEastmoneyWinRatioMapper {
    List<EastmoneyWinRatioPOJO> query9ZhuanB_Copy();
    List<EastmoneyWinRatioPOJO> query9ZhuanS_Copy();
    List<EastmoneyWinRatioPOJO> queryEtf9ZhuanB_Copy();
    List<EastmoneyWinRatioPOJO> queryEtf9ZhuanS_Copy();

    int delWinRatio();
    int saveWinRatio(List<EastmoneyWinRatioPOJO> winRatios);
    List<SpecialCarePoJo2> queryWinRatios();
}
