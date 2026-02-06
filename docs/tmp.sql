

            with tmp_ as(
                select t.stockCode, t3.stockName , t.`date` , t.volume, t.last
                       , rank() over(partition by t.stockCode order by case when t.zf_ratio < 10 then t.volume else 99999999999 end) vol_min_rank
                       , rank() over(partition by t.stockCode order by t.volume desc) vol_max_rank
                from (
                    select ten.stockCode
                           , ten.`date`
                           , ten.chg
                           , round((GREATEST(ten.`last`/(1+ten.chg/100), ten.high) / least(ten.`last`/(1+ten.chg/100), ten.low) - 1) * 100) zf_ratio
                           , ten.`last`
                           , ten.high
                           , ten.low
                           , ten.volume
                    from t_eastmoney_node ten
                    where ten.`date` >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 90 DAY), '%Y-%m-%d')
                    and DATE not like '9999%'
                    and ten.stockCode not like '0.68%'
                    and ten.stockCode not like '0.30%'
                    and ten.stockCode not like '1.68%'
                    and ten.stockCode not like '1.30%'
                    and ten.last < 80
                ) t , t_hs300 t3
                where t.stockCode = concat(t3.stockType , '.', t3.stockCode)
            )
            , tmp_stocks as (
	            select distinct t1.stockCode
	            from (select stockCode, stockName , `date` , volume from tmp_ where vol_min_rank = 1) t1,
	                 (select stockCode, stockName , `date` , volume, last from tmp_ where vol_max_rank = 1) t2
	            where t1.stockCode = t2.stockCode
	            -- and t2.`date` >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 30 DAY), '%Y-%m-%d')
	            and t2.volume > 9 * t1.volume
            )
            , tmp_avg60 as (
	            select stockCode
	                   , avg(case when date_rn <= 5 then LAST else null end) avg_5
	                   , avg(case when date_rn <= 10 then LAST else null end) avg_10
	                   , avg(case when date_rn <= 20 then LAST else null end) avg_20
	                   , avg(case when date_rn <= 60 then LAST else null end) avg_60
	                   , max(tt.`date` ) date
	            from (
		            select ten.*, t3.stockName , rank() over (partition by ten.stockCode order by ten.`date` desc) date_rn
		            from t_eastmoney_node ten, tmp_stocks t2, t_hs300 t3
		            where ten.stockCode = t2.stockCode
		            and ten.`date` >=  DATE_FORMAT(DATE_SUB(STR_TO_DATE('2026-02-22', '%Y-%m-%d'), INTERVAL 120 DAY), '%Y-%m-%d')
		            and ten.`date` <= '2026-02-22'
		            and ten.`date` not like '9999%'
		            and ten.stockCode = concat(t3.stockType , '.', t3.stockCode)
		        ) tt
		        -- where date_rn <= 60
		        group by stockCode
           )
           select t3.stockCode
                  , concat(ten.`date`, '\n',  t3.stockName) date
                  , ten.`last`
                  , concat(round(avg.avg_5, 3),'\n', round(avg.avg_10, 3),'\n', round(avg.avg_20, 3),'\n', round(avg.avg_60, 3) ) ratioB
           from tmp_avg60 avg, t_eastmoney_node ten, t_hs300 t3
           where ten.stockCode = concat(t3.stockType , '.', t3.stockCode)
           and avg.stockCode = ten.stockCode
           and ten.`date` = avg.`date`
           and ten.`low` * 0.98 < avg.avg_60
           and ten.`low` * 1.02 > avg.avg_60
           and avg.avg_60 * 1.02 < least(avg.avg_5, avg.avg_10, avg.avg_20)
           and ten.`last` < 60
           order by 1
           



select min(date), max(date) from t_eastmoney_node;

select SUBSTRING(`date` , 1, 4), count(1)
from t_eastmoney_node
group by SUBSTRING(`date` , 1, 4)
order by 1;


select * from t_eastmoney_node_9_zhuan order by 2 desc, 3;
select count(1), count(distinct _date) _date, max(distinct _date) _date_max, min(distinct _date) _date_min
       , count(distinct case when _9_zhuan = '06' then _date else null end) _06
       , count(distinct case when _9_zhuan = '07' then _date else null end) _07
       , count(distinct case when _9_zhuan = '08' then _date else null end) _08
       , count(distinct case when _9_zhuan = '09' then _date else null end) _09
       , MAX(distinct case when _9_zhuan = '09' then _date else null end) _09_date_max
       , min(distinct case when _9_zhuan = '09' then _date else null end) _09_date_min
from t_eastmoney_node_9_zhuan;

select SUBSTRING(_date, 1, 9) _year, round(sum(s_b)/count(1))
from (
	select _date, sum(case when _direction = 'S' then 1 else 0 end)/sum(case when _direction = 'S' then 0 else 1 end) s_b
	from t_eastmoney_node_9_zhuan
	group by _date
	having s_b >= 3 or s_b is NULL
) tt
group by _year
order by 1 desc;

with tmp as (
	select _date, sum(case when _direction = 'S' then 1 else 0 end)/sum(case when _direction = 'S' then 0 else 1 end) s_b
	from t_eastmoney_node_9_zhuan
	group by _date
)
select t1.*, t2.avg_s_b
from tmp t1, (select avg(s_b) avg_s_b from tmp) t2
where t1.s_b >= t2.avg_s_b * 3
order by 1 desc;


with tmp as (
	select _date, sum(case when _direction = 'S' then 1 else 0 end)/sum(case when _direction = 'S' then 0 else 1 end) s_b
	from t_eastmoney_node_9_zhuan
	group by _date
)
select t1.*, t2.avg_s_b
from tmp t1, (select avg(s_b) avg_s_b from tmp) t2
where t1.s_b <= 0.3
order by 1 desc;





select count(1) from t_eastmoney_node;



drop table t_eastmoney_node_9_zhuan;
CREATE TABLE `t_eastmoney_node_9_zhuan` (
  `id` int NOT NULL AUTO_INCREMENT,
  `_date` varchar(100) DEFAULT NULL,
  `_stockCode` varchar(200) DEFAULT NULL,
  `_direction` varchar(200) DEFAULT NULL,
  `_9_zhuan` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `t_e_n_9_z` (`_date`,`_stockCode`, _direction),
  KEY `t_e_n_d` (`_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='从东财获取的9转数据'


insert into t_eastmoney_node_9_zhuan(_date, _stockCode, _direction, _9_zhuan)
        select t9.`date`, t9.stockCode, t9._direction, t9._9_zhuan
        from (
                select t3.`date`, t3.stockCode , t3.`last`, t3.chg
                    , case  when pre_tag_6 = 0 then '06'
                            when pre_tag_7 = 0 then '07'
                            when pre_tag_8 = 0 then '08'
                            when pre_tag_9 = 0 then '09'
                            when pre_tag_10 + pre_tag_11 + pre_tag_12 + pre_tag_13 + pre_tag_14 = 5 then
                            case    when pre_tag_15 = 0 then '06'
                                    when pre_tag_16 = 0 then '07'
                                    when pre_tag_17 = 0 then '08'
                                    when pre_tag_18 = 0 then '09'
                                    when pre_tag_19 + pre_tag_20 + pre_tag_21 + pre_tag_22 + pre_tag_23 = 5 then
                                    case    when pre_tag_24 = 0 then '06'
                                            when pre_tag_25 = 0 then '07'
                                            when pre_tag_26 = 0 then '08'
                                            when pre_tag_27 = 0 then '09'
                                    else '00' end
                            else '00' end
                    else '00' end _9_zhuan
                    , 'B' _direction
                from (
                        select t2.`date` , t2.stockCode , t2.`last`, t2.chg, t2.pre_last_4
							, t2.tag_
                            , lead(t2.tag_, 1) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_1
                            , lead(t2.tag_, 2) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_2
                            , lead(t2.tag_, 3) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_3
                            , lead(t2.tag_, 4) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_4
                            , lead(t2.tag_, 5) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_5
                            , lead(t2.tag_, 6) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_6
                            , lead(t2.tag_, 7) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_7
                            , lead(t2.tag_, 8) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_8
                            , lead(t2.tag_, 9) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_9
                            , lead(t2.tag_, 10) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_10
                            , lead(t2.tag_, 11) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_11
                            , lead(t2.tag_, 12) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_12
                            , lead(t2.tag_, 13) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_13
                            , lead(t2.tag_, 14) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_14
                            , lead(t2.tag_, 15) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_15
                            , lead(t2.tag_, 16) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_16
                            , lead(t2.tag_, 17) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_17
                            , lead(t2.tag_, 18) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_18
                            , lead(t2.tag_, 19) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_19
                            , lead(t2.tag_, 20) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_20
                            , lead(t2.tag_, 21) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_21
                            , lead(t2.tag_, 22) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_22
                            , lead(t2.tag_, 23) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_23
                            , lead(t2.tag_, 24) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_24
                            , lead(t2.tag_, 25) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_25
                            , lead(t2.tag_, 26) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_26
                            , lead(t2.tag_, 27) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_27
                            , lead(t2.tag_, 28) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_28
                            , lead(t2.tag_, 29) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_29
                            , lead(t2.tag_, 30) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_30
                        from (
                                select t1.`date` , t1.stockCode , t1.`last`, t1.chg, t1.pre_last_4
                                       , case when t1.pre_last_4 > t1.`last` then 1 else 0 end tag_
                                from (
                                        select ten.`date` , ten.stockCode , ten.`last`, ten.chg
                                            , lead(ten.`last`, 4) over (partition by ten.stockCode order by ten.`date` desc) pre_last_4
                                        from t_eastmoney_node ten
                                        where ten.`date` not like '9999%'
                                        and ten.`stockCode` not like '0.30%'
                                        and ten.`stockCode` not like '0.688%'
                                        and ten.`stockCode` not like '0.689%'
                                        and ten.`stockCode` not like '1.30%'
                                        and ten.`stockCode` not like '1.688%'
                                        and ten.`stockCode` not like '1.689%'
                                        and ten.`date` >= '2014-01-01'
                                        and `date` < '2026-03-01'
                                        -- and ten.stockCode = '1.600795'
                                ) t1
                        ) t2
                ) t3
                where tag_ + pre_tag_1 + pre_tag_2 + pre_tag_3 + pre_tag_4 + pre_tag_5 = 6
            ) t9
        where _9_zhuan != '00'
        and `date` >= '2014-06-01' and `date` < '2026-01-01'
        order by 1, 2;















-- insert into t_eastmoney_node_9_zhuan(_date, _stockCode, _direction, _9_zhuan)
        select t9.`date`, t9.stockCode, t9._direction, t9._9_zhuan
        from (
                select t3.`date`, t3.stockCode , t3.`last`, t3.chg, chg_next_1, chg_next_2, chg_next_3, chg_next_4, chg_next_5, chg_next_6, chg_next_7, chg_next_8, chg_next_9, chg_next_10
                    , case  when pre_tag_6 = 0 then '06'
                            when pre_tag_7 = 0 then '07'
                            when pre_tag_8 = 0 then '08'
                            when pre_tag_9 = 0 then '09'
                            when pre_tag_10 + pre_tag_11 + pre_tag_12 + pre_tag_13 + pre_tag_14 = 5 then
                            case    when pre_tag_15 = 0 then '06'
                                    when pre_tag_16 = 0 then '07'
                                    when pre_tag_17 = 0 then '08'
                                    when pre_tag_18 = 0 then '09'
                                    when pre_tag_19 + pre_tag_20 + pre_tag_21 + pre_tag_22 + pre_tag_23 = 5 then
                                    case    when pre_tag_24 = 0 then '06'
                                            when pre_tag_25 = 0 then '07'
                                            when pre_tag_26 = 0 then '08'
                                            when pre_tag_27 = 0 then '09'
                                    else '00' end
                            else '00' end
                    else '00' end _9_zhuan
                    , 'S' _direction
                from (
                        select t2.`date` , t2.stockCode , t2.`last`, t2.chg, t2.pre_last_4, chg_next_1, chg_next_2, chg_next_3, chg_next_4, chg_next_5, chg_next_6, chg_next_7, chg_next_8, chg_next_9, chg_next_10
                            , t2.tag_
                            , lead(t2.tag_, 1) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_1
                            , lead(t2.tag_, 2) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_2
                            , lead(t2.tag_, 3) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_3
                            , lead(t2.tag_, 4) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_4
                            , lead(t2.tag_, 5) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_5
                            , lead(t2.tag_, 6) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_6
                            , lead(t2.tag_, 7) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_7
                            , lead(t2.tag_, 8) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_8
                            , lead(t2.tag_, 9) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_9
                            , lead(t2.tag_, 10) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_10
                            , lead(t2.tag_, 11) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_11
                            , lead(t2.tag_, 12) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_12
                            , lead(t2.tag_, 13) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_13
                            , lead(t2.tag_, 14) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_14
                            , lead(t2.tag_, 15) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_15
                            , lead(t2.tag_, 16) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_16
                            , lead(t2.tag_, 17) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_17
                            , lead(t2.tag_, 18) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_18
                            , lead(t2.tag_, 19) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_19
                            , lead(t2.tag_, 20) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_20
                            , lead(t2.tag_, 21) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_21
                            , lead(t2.tag_, 22) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_22
                            , lead(t2.tag_, 23) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_23
                            , lead(t2.tag_, 24) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_24
                            , lead(t2.tag_, 25) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_25
                            , lead(t2.tag_, 26) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_26
                            , lead(t2.tag_, 27) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_27
                            , lead(t2.tag_, 28) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_28
                            , lead(t2.tag_, 29) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_29
                            , lead(t2.tag_, 30) over (partition by t2.stockCode order by t2.`date` desc) pre_tag_30
                        from (
                                select t1.`date` , t1.stockCode , t1.`last`, t1.chg, t1.pre_last_4
                                    , chg_next_1
                                    , chg_next_1 + chg_next_2 chg_next_2
                                    , chg_next_1 + chg_next_2 + chg_next_3 chg_next_3
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 chg_next_4
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 chg_next_5
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 + chg_next_6 chg_next_6
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 + chg_next_6 + chg_next_7 chg_next_7
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 + chg_next_6 + chg_next_7 + chg_next_8 chg_next_8
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 + chg_next_6 + chg_next_7 + chg_next_8 + chg_next_9 chg_next_9
                                    , chg_next_1 + chg_next_2 + chg_next_3 + chg_next_4 + chg_next_5 + chg_next_6 + chg_next_7 + chg_next_8 + chg_next_9 + chg_next_10 chg_next_10
                                    , case when t1.pre_last_4 < t1.`last` then 1 else 0 end tag_
                                from (
                                        select ten.`date` , ten.stockCode , ten.`last`, ten.chg
                                            , lead(ten.`last`, 4) over (partition by ten.stockCode order by ten.`date` desc) pre_last_4
                                            , lag(ten.chg, 1) over (partition by ten.stockCode order by ten.`date` desc) chg_next_1
                                            , lag(ten.chg, 2) over (partition by ten.stockCode order by ten.`date` desc) chg_next_2
                                            , lag(ten.chg, 3) over (partition by ten.stockCode order by ten.`date` desc) chg_next_3
                                            , lag(ten.chg, 4) over (partition by ten.stockCode order by ten.`date` desc) chg_next_4
                                            , lag(ten.chg, 5) over (partition by ten.stockCode order by ten.`date` desc) chg_next_5
                                            , lag(ten.chg, 6) over (partition by ten.stockCode order by ten.`date` desc) chg_next_6
                                            , lag(ten.chg, 7) over (partition by ten.stockCode order by ten.`date` desc) chg_next_7
                                            , lag(ten.chg, 8) over (partition by ten.stockCode order by ten.`date` desc) chg_next_8
                                            , lag(ten.chg, 9) over (partition by ten.stockCode order by ten.`date` desc) chg_next_9
                                            , lag(ten.chg, 10) over (partition by ten.stockCode order by ten.`date` desc) chg_next_10
                                        from t_eastmoney_node ten
                                        where ten.`date` not like '9999%'
                                        and ten.`stockCode` not like '0.30%'
                                        and ten.`stockCode` not like '0.688%'
                                        and ten.`stockCode` not like '0.689%'
                                        and ten.`stockCode` not like '1.30%'
                                        and ten.`stockCode` not like '1.688%'
                                        and ten.`stockCode` not like '1.689%'
                                        and ten.`date` >= '2024-01-01'
                                        and ten.`date` < '2025-01-01'
                                        -- and ten.stockCode = '1.600795'
                                ) t1
                        ) t2
                ) t3
                where tag_ + pre_tag_1 + pre_tag_2 + pre_tag_3 + pre_tag_4 + pre_tag_5 = 6
            ) t9
        where _9_zhuan != '00'
        order by 1, 2;
        