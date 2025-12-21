drop table t_eastmoney_node;

CREATE TABLE `t_eastmoney_node` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` varchar(100) DEFAULT NULL,
  `stockCode` varchar(200) DEFAULT NULL,
  `open` double DEFAULT NULL,
  `last` double DEFAULT NULL,
  `high` double DEFAULT NULL,
  `low` double DEFAULT NULL,
  `volume` double DEFAULT NULL,
  `amount` double DEFAULT NULL,
  `exchangeRaw` double DEFAULT NULL,
  `infoRaw` varchar(200) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `clac_expma_5` double DEFAULT NULL,
  `clac_expma_10` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `t_e_n_d_s` (`date`,`stockCode`),
  KEY `t_e_n_d` (`date`)
) ENGINE=InnoDB COMMENT='从东财获取的hs300的历史数据'


CREATE TABLE `t_eastmoney_node_buy` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` varchar(100) DEFAULT NULL,
  `stockCode` varchar(200) DEFAULT NULL,
  `last` double DEFAULT NULL,
  `clac_expma_5` double DEFAULT NULL,
  `clac_expma_10` double DEFAULT NULL,
  `ratioB` double DEFAULT NULL,
  `rank_` int NOT NULL,
  `period_cnt_` int NOT NULL,
  `rank_limit_` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `t_q_n_d` (`date`),
  UNIQUE KEY `t_q_n_d_s` (`date`,`stockCode`)
) ENGINE=InnoDB COMMENT='可以操作买入的历史记录';