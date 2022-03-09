package com.web3j.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3j.generate.Storage;
import com.web3j.utils.Environment;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: smallming
 * @Date: 2021/11/16 7:40 下午
 * @Motto: Stay Hungry,Stay Foolish.--SteveJobs
 */
public class TransactionAndContractDemo {

    private static Web3j web3j;

    //钱包地址
    private static String address = "0x4Da8e87539E3c988D6e2431510C8fccBcaf83855";

    //账户私钥
    private static String privateKey = "95dda4681ed7b9e0eab5e21c07e495a3fdc9975e51e0a8ee7344c55cb45782cf";

    public static void main(String[] args) throws Exception {

        web3j = Web3j.build(new HttpService(Environment.RPC_URL));

        /**
         * 测试创建钱包
         */
        // createWallet("1234567890");


        /**
         *  测试发送转账交易
         */
        //sendIP1559Transaction();
        /**
         *   测试部署合约
         */
        // deployContract();


        /**
         * 加载并和合约交互
         */
        String contractAddress = "0x797C88225547126879aA4EF444A66D627E402366";
        loadContract(contractAddress);

    }

    /**
     * 转账交易
     *
     * @throws Exception
     */
    public static void sendIP1559Transaction() throws Exception {
        //转账到哪个钱包账户地址
        String toAddress = "0xAc1656e2c41711053c6Ad151bcF4A395A24C1Ee1";

        //获取证书
        Credentials credentials = getCredentials("", "", privateKey);

        /**
         * 创建并发送转账交易
         */
        TransactionReceipt transactionReceipt = Transfer.sendFundsEIP1559(
                web3j,
                credentials,
                toAddress, //toAddress
                BigDecimal.ONE.valueOf(1), //转账值
                Convert.Unit.ETHER, //转账单位
                BigInteger.valueOf(300000), // gasLimit
                DefaultGasProvider.GAS_LIMIT, //maxPriorityFeePerGas (max fee per gas transaction willing to give to miners)
                BigInteger.valueOf(3_100_000_000L) //maxFeePerGas (max fee transaction willing to pay)
        ).send();
        System.out.println("get transaction status-------------------->>>:" + transactionReceipt.isStatusOK());
    }


    /**
     * 部署合约
     *
     * @throws Exception
     */
    public static void deployContract() throws Exception {
        /**
         * 创建证书
         */
        Credentials credentials = getCredentials("", "", privateKey);
        /**
         * 获取链id
         */
        String chainId = getChainId();
        /**
         * 构建交易管理器
         */
        TransactionManager transactionManager = new RawTransactionManager(
                web3j, credentials, Long.parseLong(chainId));

        /**
         * 默认的gasPrice和gasLimit
         */
        ContractGasProvider contractGasProvider = new DefaultGasProvider();
        /**
         * 部署合约  注意：Storage类是根据sol编译后生成的abi文件以及bin文件，利用Command Line Tools生成的合约java类
         */

        Storage contract = Storage.deploy(web3j, transactionManager, contractGasProvider).send();
        System.out.println("contractAddress------->>>>>:" + contract.getContractAddress());

    }

    /**
     * 根据合约地址加载合约
     *
     * @param contractAddress
     * @throws Exception
     */
    public static void loadContract(String contractAddress) throws Exception {
        /**
         * 创建证书
         */
        Credentials credentials = getCredentials("", "", privateKey);
        String chainId = getChainId();
        /**
         * 构建交易管理器
         */
        TransactionManager transactionManager = new RawTransactionManager(
                web3j, credentials, Long.parseLong(chainId));
        /**
         * 默认的gasPrice和gasLimit
         */
        ContractGasProvider contractGasProvider = new DefaultGasProvider();
        //加载合约
        Storage contract = Storage.load(contractAddress, web3j, transactionManager, contractGasProvider);
        if (contract.isValid()) {
            /**
             * 获取合约设置的默认变量值
             */
            BigInteger retrive = contract.retrieve().send();
            System.out.println("retrieve------->>>>>:" + retrive.toString());
            /**
             * 修改合约设置的默认变量值
             * 注意：此方法调用之后会有一段时间延迟，不能立刻获取到刚设置的值
             */
            TransactionReceipt send = contract.store(new BigInteger("999")).send();

            Thread.sleep(8000);

            BigInteger retrivetSecond = contract.retrieve().send();
            System.out.println("retrieve second------->>>>>:" + retrivetSecond.toString());
        }
    }

    /**
     * 创建证书
     *
     * @param password   钱包账户密码
     * @param mnemonic   钱包助记词
     * @param privateKey 私钥
     * @return
     */
    private static Credentials getCredentials(String password, String mnemonic, String privateKey) {
        /**
         * 有两种创建证书的方式，任选一种即可
         */
        /**
         * 第一种创建方式 传入 钱包账户密码 和 钱包助记词
         */
//         Credentials credentials  = WalletUtils.loadBip39Credentials(password, mnemonic);

        /**
         * 第二种创建方式 只需要 私钥
         */
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);
        return credentials;
    }

    /**
     * 获取nonce值
     *
     * @return
     */
    private static BigInteger getNonce() throws IOException {
        BigInteger nonce;
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
        if (ethGetTransactionCount == null) return new BigInteger("");
        nonce = ethGetTransactionCount.getTransactionCount();
        return nonce;
    }

    /**
     * 获取钱包余额
     *
     * @return
     */
    private static BigInteger getBanlance() throws IOException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigInteger balance = ethGetBalance.getBalance();
        return balance;
    }

    /**
     * 获取链id
     *
     * @return
     * @throws Exception
     */
    private static String getChainId() throws Exception {
        //获取网络链ID
        String chainid = web3j.netVersion().send().getNetVersion();
        return chainid;
    }


    private static void testTokenTransaction(Web3j web3j, String fromAddress, String privateKey, String contractAddress, String toAddress, double amount, int decimals) {
        BigInteger nonce;
        EthGetTransactionCount ethGetTransactionCount = null;
        try {
            ethGetTransactionCount = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ethGetTransactionCount == null) return;
        nonce = ethGetTransactionCount.getTransactionCount();
        System.out.println("nonce " + nonce);
        BigInteger gasPrice = Convert.toWei(BigDecimal.valueOf(3), Convert.Unit.GWEI).toBigInteger();
        BigInteger gasLimit = BigInteger.valueOf(60000);
        BigInteger value = BigInteger.ZERO;
        //token转账参数
        String methodName = "transfer";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address tAddress = new Address(toAddress);
        Uint256 tokenValue = new Uint256(BigDecimal.valueOf(amount).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger());
        inputParameters.add(tAddress);
        inputParameters.add(tokenValue);
        TypeReference<Bool> typeReference = new TypeReference<Bool>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);

        byte chainId = ChainId.NONE;
        String signedData;
        try {
            signedData = signTransaction(nonce, gasPrice, gasLimit, contractAddress, value, data, chainId, privateKey);
            if (signedData != null) {
                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedData).send();
                System.out.println(ethSendTransaction.getTransactionHash());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建钱包
     *
     * @param password 密码
     */
    public static void createWallet(String password) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CipherException, JsonProcessingException {
        WalletFile walletFile;
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        walletFile = Wallet.createStandard(password, ecKeyPair);
        System.out.println("address " + walletFile.getAddress());
        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(walletFile);
        decryptWallet(jsonStr, password);
    }

    /**
     * 解密keystore 得到私钥
     *
     * @param keystore
     * @param password
     */
    public static String decryptWallet(String keystore, String password) {
        String privateKey = null;
        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        try {
            WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
            ECKeyPair ecKeyPair = null;
            ecKeyPair = Wallet.decrypt(password, walletFile);
            privateKey = ecKeyPair.getPrivateKey().toString(16);
            System.out.println("privateKey " + privateKey);
        } catch (CipherException e) {
            if ("Invalid password provided".equals(e.getMessage())) {
                System.out.println("密码错误");
            }
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    /**
     * 签名交易
     */
    public static String signTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
                                         BigInteger value, String data, int chainId, String privateKey) throws IOException {
        byte[] signedMessage;
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                value,
                data);

        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);
        if (chainId > -1) {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        } else {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        }
        String hexValue = Numeric.toHexString(signedMessage);
        return hexValue;
    }

}
