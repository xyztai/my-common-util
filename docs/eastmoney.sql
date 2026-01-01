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



drop table t_etf;
CREATE TABLE `t_etf` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `stockCode` varchar(200) DEFAULT NULL,
                         `stockName` varchar(200) DEFAULT NULL,
                         `stockType` int DEFAULT NULL,
                         `fullStockCode` varchar(200) DEFAULT NULL,
                         `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                         `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指数列表';

INSERT INTO t_etf (stockName,fullStockCode) VALUES
('001纳斯达克100ETF-159659', '0.159659'),
('002纳指100ETF-159660', '0.159660'),
('003恒生科技ETF-513130', '1.513130'),
('004日经ETF-513520', '1.513520'),
('005沙特ETF-159329', '0.159329'),
('006华宝油气LOF-162411', '0.162411'),
('007香港证券ETF-513090', '1.513090'),
('008港股通非银ETF-513750', '1.513750'),
('009H股ETF-159954', '0.159954'),
('010黄金ETF-518880', '1.518880'),
('011有色ETF-159980', '0.159980'),
('101中证1000ETF增强-561280', '1.561280'),
('102国证2000ETF-159628', '0.159628'),
('103A500ETF-159339', '0.159339'),
('104证券ETF-512880', '1.512880'),
('105航空航天ETF-159227', '0.159227'),
('106电力ETF-159611', '0.159611'),
('107电网设备ETF-159326', '0.159326'),
('108石油天然气ETF-159588', '0.159588'),
('109储能电池ETF-159566', '0.159566'),
('110半导体设备ETF-159516', '0.159516'),
('111科创芯片ETF-588200', '1.588200'),
('112机器人ETF-562500', '1.562500'),
('113卫星ETF-159206', '0.159206'),
('114医疗创新ETF-516820', '1.516820'),
('115电池ETF-159755', '0.159755'),
('116军工ETF-512660', '1.512660'),
('117空天军工LOF-160643', '0.160643'),
('118黄金股ETF-517520', '1.517520'),
('119游戏ETF-159869', '0.159869'),
('120软件ETF-515230', '1.515230'),
('121光伏ETF-515790', '1.515790'),
('122科创50ETF-588000', '1.588000'),
('1235G通信ETF-515050', '1.515050'),
('124农业ETF-159825', '0.159825'),
('125基建ETF-516950', '1.516950'),
('126旅游ETF-159766', '0.159766'),
('127银行ETF天弘-515290', '1.515290');



CREATE TABLE `t_eastmoney_node_etf` (
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
                                        `chg` double DEFAULT NULL,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `t_e_n_d_s` (`date`,`stockCode`),
                                        KEY `t_e_n_d` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='从东财获取etf的历史数据';
