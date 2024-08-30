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