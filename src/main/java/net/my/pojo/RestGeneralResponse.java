package net.my.pojo;

public class RestGeneralResponse<T> extends BaseResponse {
    public RestGeneralResponse(Integer errCode, String errMsg) {
        super(errCode, errMsg);
    }

    public T data;

    public RestGeneralResponse(T data) {
        super(ERROR_OK, "OK");
        this.data = data;
    }

    public RestGeneralResponse(int code, String msg, T data) {
        super(code, msg);
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RestGeneralResponse {");
        if(getErrCode() != null) {
            builder.append("errCode=").append(getErrCode()).append(", ");
        }
        if(getErrMsg() != null) {
            builder.append("errMsg=").append(getErrMsg()).append(", ");
        }
        if(data != null) {
            builder.append("data=").append(data);
        }
        builder.append("}");
        return builder.toString();
    }

    public static <T> RestGeneralResponse<T> of(T data) {
        return new RestGeneralResponse<>(data);
    }
}
