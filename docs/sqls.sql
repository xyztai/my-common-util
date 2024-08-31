-- 根据参数计算操作
delete from t_history_expect;
insert into t_history_expect(time, type, close_price, b_action, s_action)
select t3.time, t3.type, t3.close_price, 500 buy_action, -500 sell_action
from t_para_daily t1, t_data_calc t2, t_raw_close_price t3
where t1.type = t2.`type`
and t1.time = t2.time
and t2.`time` = t3.`time`
and t2.`type` = t3.`type`
-- and t1.type = 'hs300'


insert into t_history_expect(time, type, close_price, b_action, s_action, b_ratio_ans, b_ratio_para, s_ratio_ans, s_ratio_para)
select
    t11.`time`
     -- , case when t11.b_ratio_ans - 1 >= b_ratio_500 then 'buy' else 'sell' end as `oper_dir`
     -- , case when t11.b_ratio_ans - 1 >= b_ratio_500 then t11.b_ratio_ans else t11.s_ratio_ans end ratio_c
     , t13.type
     -- , t13.name
     , t14.close_price
     , case when t11.b_ratio_ans - 1 >= b_ratio_5000 then 5000
            when t11.b_ratio_ans - 1 >= b_ratio_4000 then 4000
            when t11.b_ratio_ans - 1 >= b_ratio_3000 then 3000
            when t11.b_ratio_ans - 1 >= b_ratio_2000 then 2000
            when t11.b_ratio_ans - 1 >= b_ratio_1000 then 1000
            else null
    end as `b_action`
     -- , abs(t11.s_ratio_ans - 1), s_ratio_1_in_4_start
     , case when t11.s_ratio_ans - 1 >= s_ratio_1_in_1 then 1
            when t11.s_ratio_ans - 1 >= s_ratio_1_in_2 then 1
            when t11.s_ratio_ans - 1 >= s_ratio_1_in_3 then 0.5
            when t11.s_ratio_ans - 1 >= s_ratio_1_in_4 then 0.33
            else null
    end as `s_action`
     , t11.b_ratio_ans, t12.b_ratio_para
     , t11.s_ratio_ans, t12.s_ratio_para
from
    (
        select
            t1.`time`
             , t1.`type`
             , round(t1.`expma_5`/t1.`expma_37`, 5) s_ratio_ans
             , round(t1.`expma_37`/t1.`expma_5`, 5) b_ratio_ans
        from t_data_calc t1
    ) t11,
    (
        select
            t2.type
             , t2.time
             , t2.b_ratio b_ratio_para
             , 0.5*(t2.b_ratio-1) b_ratio_1000
             , 0.6*(t2.b_ratio-1) b_ratio_2000
             , 0.7*(t2.b_ratio-1) b_ratio_3000
             , 0.9*(t2.b_ratio-1) b_ratio_4000
             , 1.1*(t2.b_ratio-1) b_ratio_5000
             , t2.s_ratio s_ratio_para
             , 0 s_ratio_1_in_4
             , 0.3*(t2.s_ratio-1) s_ratio_1_in_3
             , 0.7*(t2.s_ratio-1) s_ratio_1_in_2
             , 1*(t2.s_ratio-1) s_ratio_1_in_1
        from t_para_daily t2
    ) t12,
    (
        select *
        from t_type t3
    ) t13,
    t_raw_close_price t14
where t11.type = t12.type
  and t11.time = t12.time
  and t11.type = t13.type
  and t11.time = t14.time
  and t11.type = t14.type
  and ((t11.b_ratio_ans - 1 >= b_ratio_1000) or (t11.s_ratio_ans - 1 >= s_ratio_1_in_4))
order by 1 desc, 2, 3 desc;
