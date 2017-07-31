package io.mycat.backend.callback;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.buffer.MycatByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 认证响应处理器
 *
 * @author ynfeng
 */
@Deprecated
public class AuthenticateRespCallback extends ResponseCallbackAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticateRespCallback.class);

    @Override
    public void handleResponse(MySQLBackendConnection conn, MycatByteBuffer dataBuffer, byte packageType, int pkgStartPos, int pkgLen) throws IOException {
    }
}
