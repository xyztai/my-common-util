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
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `t_e_n_d_s` (`date`,`stockCode`)
) ENGINE=InnoDB COMMENT='从东财获取的hs300的历史数据';