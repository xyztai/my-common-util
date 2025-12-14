package net.my.controller;

public class MairuiController {
    /**
     * 接口文档： https://www.mairui.club/hsdata
     博客网址：https://www.cnblogs.com/ljbguanli/p/19123757
     获取证书：https://www.mairui.club/gratis：licence证书：FAF714CE-40CF-40AC-9C7A-A1D0BB5DDDAA
     获取证券列表：http://api.mairuiapi.com/hslt/list/FAF714CE-40CF-40AC-9C7A-A1D0BB5DDDAA
     获取不复权的历史记录： https://api.mairuiapi.com/hsstock/history/000001.SZ/d/n/FAF714CE-40CF-40AC-9C7A-A1D0BB5DDDAA?lt=2000
     获取前复权的历史记录： https://api.mairuiapi.com/hsstock/history/000001.SZ/d/fr/FAF714CE-40CF-40AC-9C7A-A1D0BB5DDDAA?lt=2000
     * 最新分时交易
     * API接口：https://api.mairuiapi.com/hsstock/latest/股票代码.市场（如000001.SZ）/分时级别(如d)/除权方式/您的licence?lt=最新条数(如10)
     * 接口说明：根据《股票列表》得到的股票代码和分时级别获取最新交易数据，交易时间升序。
     * 目前分时级别支持5分钟、15分钟、30分钟、60分钟、日线、周线、月线、年线，对应的请求参数分别为5、15、30、60、d、w、m、y，
     * 日线以上除权方式有不复权、前复权、后复权、等比前复权、等比后复权，对应的参数分别为n、f、b、fr、br，分钟级无除权数据，对应的参数为n。
     * 同时可以指定获取数据条数，例如指定lt=10，则获取最新的10条数据。
     */
}
