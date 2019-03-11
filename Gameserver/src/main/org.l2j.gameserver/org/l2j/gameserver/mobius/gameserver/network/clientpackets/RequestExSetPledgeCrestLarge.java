package org.l2j.gameserver.mobius.gameserver.network.clientpackets;

import org.l2j.gameserver.mobius.gameserver.data.sql.impl.CrestTable;
import org.l2j.gameserver.mobius.gameserver.model.ClanPrivilege;
import org.l2j.gameserver.mobius.gameserver.model.L2Clan;
import org.l2j.gameserver.mobius.gameserver.model.L2Crest;
import org.l2j.gameserver.mobius.gameserver.model.L2Crest.CrestType;
import org.l2j.gameserver.mobius.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.mobius.gameserver.network.InvalidDataPacketException;
import org.l2j.gameserver.mobius.gameserver.network.SystemMessageId;

import java.nio.ByteBuffer;

/**
 * Format : chdb c (id) 0xD0 h (subid) 0x11 d data size b raw data (picture i think ;) )
 *
 * @author -Wooden-
 */
public final class RequestExSetPledgeCrestLarge extends IClientIncomingPacket {
    private int _length;
    private byte[] _data = null;

    @Override
    public void readImpl(ByteBuffer packet) throws InvalidDataPacketException {
        _length = packet.getInt();
        if (_length > 2176) {
            throw new InvalidDataPacketException();
        }

        _data = new byte[_length];
        packet.get(_data);
    }

    @Override
    public void runImpl() {
        final L2PcInstance activeChar = client.getActiveChar();
        if (activeChar == null) {
            return;
        }

        final L2Clan clan = activeChar.getClan();
        if (clan == null) {
            return;
        }

        if ((_length < 0) || (_length > 2176)) {
            client.sendPacket(SystemMessageId.THE_SIZE_OF_THE_UPLOADED_SYMBOL_DOES_NOT_MEET_THE_STANDARD_REQUIREMENTS);
            return;
        }

        if (clan.getDissolvingExpiryTime() > System.currentTimeMillis()) {
            client.sendPacket(SystemMessageId.AS_YOU_ARE_CURRENTLY_SCHEDULE_FOR_CLAN_DISSOLUTION_YOU_CANNOT_REGISTER_OR_DELETE_A_CLAN_CREST);
            return;
        }

        if (!activeChar.hasClanPrivilege(ClanPrivilege.CL_REGISTER_CREST)) {
            client.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
        }

        if (_length == 0) {
            if (clan.getCrestLargeId() != 0) {
                clan.changeLargeCrest(0);
                client.sendPacket(SystemMessageId.THE_CLAN_MARK_HAS_BEEN_DELETED);
            }
        } else {
            if (clan.getLevel() < 3) {
                client.sendPacket(SystemMessageId.A_CLAN_CREST_CAN_ONLY_BE_REGISTERED_WHEN_THE_CLAN_S_SKILL_LEVEL_IS_3_OR_ABOVE);
                return;
            }

            final L2Crest crest = CrestTable.getInstance().createCrest(_data, CrestType.PLEDGE_LARGE);
            if (crest != null) {
                clan.changeLargeCrest(crest.getId());
                client.sendPacket(SystemMessageId.THE_CLAN_MARK_WAS_SUCCESSFULLY_REGISTERED_THE_SYMBOL_WILL_APPEAR_ON_THE_CLAN_FLAG_AND_THE_INSIGNIA_IS_ONLY_DISPLAYED_ON_ITEMS_PERTAINING_TO_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_CASTLE);
            }
        }
    }
}