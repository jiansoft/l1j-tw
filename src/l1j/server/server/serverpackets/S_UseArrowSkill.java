/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server.serverpackets;

import java.util.concurrent.atomic.AtomicInteger;

import l1j.server.server.Opcodes;
import l1j.server.server.model.L1Character;

// Referenced classes of package l1j.server.server.serverpackets:
// ServerBasePacket

public class S_UseArrowSkill extends ServerBasePacket {

	private static final String S_USE_ARROW_SKILL = "[S] S_UseArrowSkill";

	private static AtomicInteger _sequentialNumber = new AtomicInteger(0);

	private byte[] _byte = null;
	private int _attackerId;
	private int _targetId;
	private int _actId;
	private int _damage;
	private int _spellGfx;
	private int _srcX;
	private int _srcY;
	private int _destX;
	private int _destY;

	public S_UseArrowSkill(L1Character cha, int targetobj, int x, int y, int[] data) { // data = {actid, dmg, spellgfx}
		_attackerId = cha.getId();
		_targetId = targetobj;
		_actId = data[0];
		_damage = data[1];
		_spellGfx = data[2];
		_srcX = cha.getX();
		_srcY = cha.getY();
		_destX = x;
		_destY = y;
		writeC(Opcodes.S_OPCODE_ATTACKPACKET);
		writeC(data[0]); // actid
		writeD(cha.getId());
		writeD(targetobj);
		writeH(data[1]); // dmg
		writeC(cha.getHeading());
		writeD(_sequentialNumber.incrementAndGet());
		writeH(data[2]); // spellgfx
		writeC(0); // use_type 箭
		writeH(cha.getX());
		writeH(cha.getY());
		writeH(x);
		writeH(y);
		writeC(0);
		writeC(0);
		writeC(0); // 0:none 2:爪痕 4:雙擊 8:鏡返射
	}

	@Override
	public byte[] getContent() {
		if (_byte == null) {
			_byte = _bao.toByteArray();
		}
		else {
			int seq = 0;
			synchronized (this){
				seq = _sequentialNumber.incrementAndGet();
			}
			_byte[13] = (byte) (seq & 0xff);
			_byte[14] = (byte) (seq >> 8 & 0xff);
			_byte[15] = (byte) (seq >> 16 & 0xff);
			_byte[16] = (byte) (seq >> 24 & 0xff);
		}
		return _byte;
	}

	@Override
	public String getType() {
		return S_USE_ARROW_SKILL;
	}

	@Override
	public String getParams() {
		return "attackerId=" + _attackerId + ", targetId=" + _targetId + ", actId=" + _actId + ", dmg=" + _damage + ", spellGfx=" + _spellGfx + ", src=(" + _srcX + "," + _srcY + "), dest=(" + _destX + "," + _destY + ")";
	}
}
