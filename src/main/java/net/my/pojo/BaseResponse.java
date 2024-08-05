package net.my.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse {
    private Integer errCode;
    private String errMsg;
    private String traceId;

    public static final int ERROR_OK = 0;

    public static final BaseResponse OK = new BaseResponse(ERROR_OK, "OK", null);

    public BaseResponse(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
}
