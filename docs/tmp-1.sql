


select *
from t_eastmoney_node_etf tene
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
    from t_eastmoney_node_etf tene, t_etf te
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
from t_eastmoney_node_etf tene, t_etf te
where tene .stockCode = concat(te.stockType, '.', te.stockCode )
-- and tene.date = '2026-02-13'
  and tene.stockCode = '0.159206'
  and `date` not like '9999%'
-- and `date` > '2025-10-01'
-- and chg > 10
order by 1 desc;















with tmp_69 as (
    select t1.`_date` name
         , sum(case when `_direction` = 'S' and `_9_zhuan` = '06' then 1 else 0 end) value1
         , sum(case when `_direction` = 'S' and `_9_zhuan` = '07' then 1 else 0 end) value2
         , sum(case when `_direction` = 'S' and `_9_zhuan` = '08' then 1 else 0 end) value3
         , sum(case when `_direction` = 'S' and `_9_zhuan` = '09' then 1 else 0 end) value4
         , sum(case when `_direction` = 'B' and `_9_zhuan` = '06' then 1 else 0 end) value5
         , sum(case when `_direction` = 'B' and `_9_zhuan` = '07' then 1 else 0 end) value6
         , sum(case when `_direction` = 'B' and `_9_zhuan` = '08' then 1 else 0 end) value7
         , sum(case when `_direction` = 'B' and `_9_zhuan` = '09' then 1 else 0 end) value8
         , round(cnt_win/cnt_all * 100) value9
         -- , case when round(cnt_win/cnt_all * 100) <= 75 then 0 else round(cnt_win/cnt_all * 100) end value9
         , sum(case when `_direction` = 'S' and `_9_zhuan` in ('06', '07', '08', '09') then 1
                    when `_direction` = 'B' and `_9_zhuan` in ('06', '07', '08', '09') then -1
                    else 0 end) value10
         -- , round(cnt_win/cnt_all * 100), cnt_all
    from t_eastmoney_node_9_zhuan t1, t_eastmoney_node_avg_statis tenas
    where t1.`_date` > '2025-01-01'
      and t1.`_date` = tenas .`_date`
    group by t1.`_date`, value9
    order by 1 desc
    limit 250
    )
   , tmp_ans as (
select t.name
        , t.value1
        , t.value2
        , t.value3
        , t.value4
        , t.value5
        , t.value6
        , t.value7
        , t.value8
        , t.value9
        , t.value10
        , rank() over (order by t.value10) rank_
from tmp_69 t
    )
select t.name
     , t.value1
     , t.value2
     , t.value3
     , t.value4
     , t.value5
     , t.value6
     , t.value7
     , t.value8
     , t.value9
     , t.value10
     , t1.value10 value11
     , t2.value10 value12
from tmp_ans t
   , (select max(value10) value10 from tmp_ans where rank_ <= 250 * 0.2) t1
   , (select min(value10) value10 from tmp_ans where rank_ >= 250 * 0.8) t2
order by 1