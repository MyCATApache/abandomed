package io.mycat.engine.dataChannel;

/**
 * 透传模式
 * @author yanjunli
 *
 */
public enum TransferMode {
	SHORT_HALF_PACKET,     //短半包透传, 当前包长度不能够解析出报文头
	LONG_HALF_PACKET,      //半包透传状态  当前包长度可以解析出报文头,但是不够完整的一个包的长度
	COMPLETE_PACKET,       //全包透传状态
	NONE                   //当前不处于透传模式
}
