package com.platform.addressBook.event;

import com.platform.addressBook.AddressBookItem;
import com.yerbaswallet.core.BRCoreAddress;

public interface OnAddressClickedListener {

    void onAddressClicked(AddressBookItem address);

    void onAddressClicked(BRCoreAddress address);
}
