/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_yerbas_core_BRCoreTransactionAsset */

#ifndef _Included_com_yerbas_core_BRCoreTransactionAsset
#define _Included_com_yerbas_core_BRCoreTransactionAsset
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    createTransactionAsset
 * Signature: (J[B)J
 */
JNIEXPORT jobject JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_getType
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setType
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_setType
        (JNIEnv *env, jobject thisObject, jobject typeObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getName
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setAddress
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_setName
        (JNIEnv *env, jobject thisObject, jstring nameObject);


JNIEXPORT jstring JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_getName(JNIEnv *env, jobject instance);


JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_setName(JNIEnv *env, jobject thisObject,
                                                       jstring nameObject);

JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_setNamelen(JNIEnv *env, jobject instance,
                                                          jint namelen);

JNIEXPORT jint JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_getNameLen(JNIEnv *env, jobject instance);


JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionAsset_setNamelen(JNIEnv *env, jobject instance,
                                                          jint namelen);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getAmount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getAmount
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setAmount
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_yerbaswallet_core_BRCoreTransactionOutput_setAmount
        (JNIEnv *env, jobject thisObject, jlong amount);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getUnit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getUnit
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setUnit
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_yerbaswallet_core_BRCoreTransactionOutput_setUnit
        (JNIEnv *env, jobject thisObject, jlong unit);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getReissuable
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getReissuable
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setReissuable
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_yerbaswallet_core_BRCoreTransactionOutput_setReissuable
        (JNIEnv *env, jobject thisObject, jlong reissuable);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getHasIPFS
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getHasIPFS
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setHasIPFS
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_yerbaswallet_core_BRCoreTransactionOutput_setHasIPFS
        (JNIEnv *env, jobject thisObject, jlong hasIPFS);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    getIPFSHash
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_getIPFSHash
        (JNIEnv *env, jobject thisObject);

/*
 * Class:     com_yerbas_core_BRCoreTransactionAsset
 * Method:    setIPFSHash
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_yerbaswallet_core_BRCoreTransactionOutput_setIPFSHash
        (JNIEnv *env, jobject thisObject, jstring IPFSHashObject);

#ifdef __cplusplus
}
#endif
#endif
