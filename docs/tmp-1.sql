


select *
from t_etf_raw tene
where tene.stockCode = '0.159659'
order by 1 desc;




select concat(te.stockType, '.', te.stockCode ) sc, te.stockName
from ;


-- select tene.*, te.stockName
-- select min(volume) vol_min, max(volume) vol_max, round(max(volume)/min(volume), 1)
with tmp_etf as (
    select rank() over (partition by tene.stockCode order by tene.volume) - 1 rank_min
	       , rank() over (partition by tene.stockCode order by tene.volume desc) - 1 rank_max
	       , tene.*, te.stockName
    from t_etf_raw tene, t_etf te
    where tene .stockCode = concat(te.stockType, '.', te.stockCode )
      -- and tene.date = '2026-02-13'
      -- and tene.stockCode = '0.159206'
      and `date` not like '9999%'
      and `date` > DATE_FORMAT(DATE_ADD(STR_TO_DATE('2025-03-01', '%Y-%m-%d'), INTERVAL -90 DAY), '%Y-%m-%d')
      and `date` < '2025-03-01'
)
select te.date
     , te.stockCode
     , round(te.volume/temin.volume, 0) vol_multi
     , te.chg
     , te.stockCode
     , temin.date
     , temin.volume
     , temin.stockCode
     , temin.stockName
from tmp_etf te
   , (select stockCode, stockName, `date`, volume from tmp_etf where rank_min = 0 and chg < 9 and chg > -9) temin
where te.volume > temin.volume * 10
  and te.volume < temin.volume * 50
  and trim(te.stockCode) = trim(temin.stockCode)
order by stockName,  te.date desc;



select tene.*
from t_etf_raw tene, t_etf te
where tene .stockCode = concat(te.stockType, '.', te.stockCode )
-- and tene.date = '2026-02-13'
  and tene.stockCode = '0.159206'
  and `date` not like '9999%'
-- and `date` > '2025-10-01'
-- and chg > 10
order by 1 desc;






