DIR=$(shell pwd)
JAVA_DIR=${JAVA_HOME}

CINC_DIR=/usr/include

JNI_LIB=libCore.jnilib

JNI_SRCS=com_yerbas_core_BRCoreAddress.c \
	com_yerbas_core_BRCoreChainParams.c \
	com_yerbas_core_BRCoreJniReference.c \
	com_yerbas_core_BRCoreKey.c \
	com_yerbas_core_BRCoreMasterPubKey.c \
	com_yerbas_core_BRCoreMerkleBlock.c \
	com_yerbas_core_BRCorePaymentProtocol.c \
	com_yerbas_core_BRCorePeer.c \
	com_yerbas_core_BRCorePeerManager.c \
	com_yerbas_core_BRCoreTransaction.c \
	com_yerbas_core_BRCoreTransactionInput.c \
	com_yerbas_core_BRCoreTransactionOutput.c \
	com_yerbas_core_BRCoreTransactionAsset.c \
	com_yerbas_core_BRCoreWallet.c \
	BRCoreJni.c

JNI_OBJS=$(JNI_SRCS:.c=.o)

# JNI Header Files that we are interest in keeping (that are not empty)
JNI_HDRS=$(JNI_SRCS:.c=.h)

JAVA_SRCS=root/com/yerbas/core/BRCoreAddress.java \
	root/com/yerbas/core/BRCoreChainParams.java \
	root/com/yerbas/core/BRCoreJniReference.java \
	root/com/yerbas/core/BRCoreKey.java \
	root/com/yerbas/core/BRCoreMasterPubKey.java \
	root/com/yerbas/core/BRCoreMerkleBlock.java \
	root/com/yerbas/core/BRCorePaymentProtocolEncryptedMessage.java \
	root/com/yerbas/core/BRCorePaymentProtocolInvoiceRequest.java \
	root/com/yerbas/core/BRCorePaymentProtocolMessage.java \
	root/com/yerbas/core/BRCorePaymentProtocolPayment.java \
	root/com/yerbas/core/BRCorePaymentProtocolACK.java \
	root/com/yerbas/core/BRCorePaymentProtocolRequest.java \
	root/com/yerbas/core/BRCorePeer.java \
	root/com/yerbas/core/BRCorePeerManager.java \
	root/com/yerbas/core/BRCoreTransaction.java \
	root/com/yerbas/core/BRCoreTransactionInput.java \
	root/com/yerbas/core/BRCoreTransactionOutput.java \
	root/com/yerbas/core/BRCoreTransactionAsset.java \
	root/com/yerbas/core/MyTransactionAsset.java \
	root/com/yerbas/core/BRCoreWallet.java \
	root/com/yerbas/core/BRCoreWalletManager.java \
	root/com/yerbas/core/test/BRWalletManager.java

JAVA_OBJS=$(JAVA_SRCS:.java=.class)

CORE_SRCS=/core/BRAddress.c \
	/core/BRBIP32Sequence.c \
	/core/BRBIP38Key.c \
	/core/BRBIP39Mnemonic.c \
	/core/BRBase58.c \
	/core/BRBech32.c \
	/core/BRBloomFilter.c \
	/core/BRCrypto.c \
	/core/BRKey.c \
	/core/BRMerkleBlock.c \
	/core/BRPaymentProtocol.c \
	/core/BRPeer.c \
	/core/BRPeerManager.c \
	/core/BRSet.c \
	/core/BRTransaction.c \
	/core/BRWallet.c \

CORE_OBJS=$(CORE_SRCS:.c=.o)

CFLAGS=-I$(JAVA_HOME)/include \
	-I$(JAVA_HOME)/include/darwin \
	-I$(CINC_DIR) \
	-I$(CINC_DIR)/malloc \
	-I/core \
	-I/core/secp256k1 \
	-Wno-nullability-completeness -Wno-format-extra-args -Wno-unknown-warning-option

compile: $(JNI_LIB) java_comp

test: $(JNI_LIB) java_comp
	java -Xss1m -Dwallet.test -classpath build -Djava.library.path=. \
		 yerbaswallet.core.test.BRWalletManager $(ARGS) # -D.

debug: $(JNI_LIB) java_comp
	java -Xss1m -Xdebug -Xrunjdwp:transport=dt_socket,address=8008,server=y,suspend=n \
		 -Dwallet.test -classpath build -Djava.library.path=. \
		 yerbaswallet.core.test.BRWalletManager $(ARGS) # -D.

$(JNI_LIB): $(JNI_OBJS) $(CORE_OBJS)
	cc -dynamiclib -o $(JNI_LIB) $(JNI_OBJS) $(CORE_OBJS)

java_comp:	FORCE
	@mkdir -p build
	javac -d build $(JAVA_SRCS)

jni_hdr: java_comp
	@(cd build/com/yerbas/core; \
	  for class in BRCore*.class; do \
	      javah -jni -d $(DIR) -classpath $(DIR)/build yerbaswallet.core.$${class%%.class}; \
	  done)
	@rm -f com_yerbas_core_BRCoreWalletManager.h com_yerbas_core_BRCore*_*.h .h

clean:
	rm -rf build $(JNI_OBJS) $(CORE_OBJS) $(JAVA_OBJS) $(JNI_LIB)

FORCE:

#	-Wno-nullability-completeness


# Generate Headers:
# 	javac Foo.java
# 	javah -jni -d <location> Foo
# Implement Foo.c
# Generate Foo.so
# 	cc -I<path to jni.h> -I<path to jni_md.h> -I<path to std> -c Foo.c
# 	cc -dynamiclib -o libFoo.jnilib Foo.o Bar.o

# 	<path-to-jni-md>=/Library/Java/JavaVirtualMachines/jdk1.8.0_151.jdk/Contents/Home/include/darwin
# 	<path-to-jni>=/Library/Java/JavaVirtualMachines/jdk1.8.0_151.jdk/Contents/Home/include
# 	<path-to-std>=/…/Xcode.app/Contents/Dev…/Platforms/MacOSX.platform/Dev…/SDKs/MacOSX.sdk/usr/include
# Run Foo
# 	java -Djava.library.path=/Users/ebg Foo
#

# (cd ${APP}/app/build/intermediates/classes/debug/com/yerbas/core; for class in BRCore*.class; do \
#     javah -jni -d ${APP}/app/src/main/cpp/yerbas-core/Java/ \
#	-classpath ${APP}/app/build/intermediates/classes/debug/ \
#	yerbaswallet.core.${class%%.class}; \
#	done)
# 15RBcXQMTfebbAfUFeBbcDfs1fVvPayWdU
