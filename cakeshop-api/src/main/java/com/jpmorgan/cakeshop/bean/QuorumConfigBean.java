/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jpmorgan.cakeshop.bean;

import com.jpmorgan.cakeshop.util.FileUtils;
import static com.jpmorgan.cakeshop.util.FileUtils.expandPath;

import java.io.*;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.JdbcConfig;
import com.quorum.tessera.config.SslAuthenticationMode;
import com.quorum.tessera.config.SslTrustMode;
import com.quorum.tessera.config.builder.ConfigBuilder;
import com.quorum.tessera.config.builder.KeyDataBuilder;
import com.quorum.tessera.config.util.jaxb.MarshallerBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

@Component
public class QuorumConfigBean implements InitializingBean { // TODO: rename to ConstellationConfigBean

    private static final Logger LOG = LoggerFactory.getLogger(QuorumConfigBean.class);

    private static final String QUORUM_LINUX_COMMAND = "quorum/linux/geth";
    private static final String QUORUM_MAC_COMMAND = "quorum/mac/geth";
    private static final String CONSTELLATION_LINUX_COMMAND = "quorum/constellation/linux/constellation-node";
    private static final String CONSTELLATION_MAC_COMMAND = "quorum/constellation/mac/constellation-node";
    private static final String CONSTELLATION_LINUX_KEYGEN = "quorum/constellation/linux/constellation-node";
    private static final String CONSTELLATION_LINUX_KEYGEN_PARAMS = "--generatekeys=%s";
    private static final String CONSTELLATION_MAC_KEYGEN = "quorum/constellation/mac/constellation-node";
    private static final String CONSTELLATION_MAC_KEYGEN_PARAMS = "--generatekeys=%s";
    private static final String TESSERA_LINUX_KEYGEN = "quorum/tessera/linux/tessera-node";
    private static final String TESEERA_LINUX_KEYGEN_PARAMS = "-keygen -filename %s";
    private final String CONSTELLATION_URL = StringUtils.isNotBlank(System.getProperty("geth.constellation.url"))
            ? System.getProperty("geth.constellation.url") : "http://127.0.0.1:9000";
    private final String TESSERA_URL = StringUtils.isNotBlank(System.getProperty("geth.tessera.url"))
    ? System.getProperty("geth.tessera.url") : "http://127.0.0.1:9000";

    private final String EMBEDDED_NODE = null != System.getProperty("geth.node") ? System.getProperty("geth.node") : null;
    private static final String TESSERA_LINUX_COMMAND_PATH = "quorum/tessera/linux/";

    private String quorumPath;
    private String constellationPath;
    private String keyGen;
    private String keyGenParams;
    private String tesseraKeyGen;
    private String tesseraKeyParams;
    private String constellationConfig;


    private String tesseraPath;
    private String tesseraConfig;


    @Value("${geth.bootnodes.list:\"\"}")
    private String bootNodes;
    @Value("${geth.bootnode.key:\"\"}")
    private String bootNodeKey;
    @Value("${geth.bootnode.address:\"\"}")
    private String bootNodeAddress;
    @Value("${geth.boot.node:false}")
    private Boolean isBootNode;

    /**
     * @return the quorumPath
     */
    public String getQuorumPath() {
        return quorumPath;
    }

    /**
     * @param quorumPath the quorumPath to set
     */
    private void setQuorumPath(String quorumPath) {
        this.quorumPath = quorumPath;
    }

    /**
     * @return the constallationPath
     */
    public String getConstellationPath() {
        return constellationPath;
    }

    /**
     * @param constellationPath the constallationPath to set
     */
    private void setConstellationPath(String constallationPath) {
        this.constellationPath = constallationPath;
    }

    public String getTesseraPath() {
      return tesseraPath;
    }

    public void setTesseraPath(String tesseraPath) {
      this.tesseraPath = tesseraPath;
    }

    /**
     * @return the keyGen
     */
    public String getKeyGen() {
        return keyGen;
    }

    /**
     * @param keyGen the keyGen to set
     */
    private void setKeyGen(String keyGen) {
        this.keyGen = keyGen;
    }

    /**
     * @return the keyGenParams
     */
    public String getKeyGenParams() {
        return keyGenParams;
    }

    public String getKeyGenParams(String destination) {
        return keyGenParams.concat(" --workdir=" + destination); // TODO: passing this fails keygen from war file
    }

    /**
     * @param keyGenParams Commandline arguments for the key generator executable
     */
    private void setKeyGenParams(String keyGenParams) {
        this.keyGenParams = keyGenParams;
    }

    public String getConstellationConfigPath() {
        return constellationConfig;
    }

    public String getTesseraConfigPath() {
      return tesseraConfig;
    }


    /**
     * @return the bootNode
     */
    public String getBootNodes() {
        return bootNodes;
    }

    /**
     * @return the bootNodeKey
     */
    public String getBootNodeKey() {
        return bootNodeKey;
    }

    /**
     * @return the bootNodeAddress
     */
    public String getBootNodeAddress() {
        return bootNodeAddress;
    }

    /**
     * @return the isBootNode
     */
    public Boolean isBootNode() {
        return isBootNode;
    }

    public String getTesseraKeyGen() {
      return tesseraKeyGen;
    }

    public void setTesseraKeyGen(String tesseraKeyGen) {
      this.tesseraKeyGen = tesseraKeyGen;
    }

    public String getTesseraKeyParams() {
      return tesseraKeyParams;
    }

    public void setTesseraKeyParams(String tesseraKeyParams) {
      this.tesseraKeyParams = tesseraKeyParams;
    }

    public void createConstellationKeys(final String keyName, final String destination) throws IOException, InterruptedException {
        constellationConfig = destination;
        File dir = new File(destination);

      if (!keysExists(keyName, destination, dir)) {
            //create keys
           createKeys(getKeyGen(),getKeyGenParams(),destination,keyName);
      }
    }

    private void createKeys(String keyGenCommand, String keyGenParams,String destination,String keyName) throws IOException, InterruptedException {
      String.format(keyGenParams, expandPath(destination, keyName));
      System.out.println("******************* expand path"+expandPath(destination, keyName));
      System.out.println("********************* generate command for key at "+      String.format(keyGenParams, expandPath(destination, keyName)));
      System.out.println("************************destination************"+destination);
      System.out.println("***********argsss*********"+Arrays.toString((String.format(keyGenParams, expandPath(destination, keyName)).split("\\s"))));

      List<String> commandArgs = new ArrayList<String>();
      commandArgs.add(keyGenCommand);
      commandArgs.addAll(Arrays.asList(
        String.format(keyGenParams, expandPath(destination, keyName)).split("\\s")));

      for(int i=0 ;i < commandArgs.size();i++){
        System.out.println(commandArgs.get(i));
      }
      ProcessBuilder pb = new ProcessBuilder(commandArgs);
      pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
      LOG.info("keygen command: " +  String.join(" ", pb.command()));
      Process process = pb.start();
      SendReturnToProcess(process);
      int ret = process.waitFor();
      if (ret != 0) {
          LOG.error("Failed to generate keys with code " + ret);
      }

      if (process.isAlive()) {
          process.destroy();
      }
    }

  public void createTesseraKeys(final String keyName, final String destination) throws IOException, InterruptedException{
      File dir = new File(destination);
        if (!keysExists(keyName, destination, dir)) {
          //create keys
          createKeys(getTesseraKeyGen(),getTesseraKeyParams(),destination,keyName);
        }


    }

    private Boolean keysExists(String keyName, String destination, File dir) throws IOException {
    if (!dir.exists()) {
        dir.mkdirs();
        return false;
    } else {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(destination), keyName + ".{key,pub}")) {
            int found = 0;
            for (Path entry: stream) {
                LOG.info("found key file: " + entry);
                found++;
                if (found == 2) { return true;}
            }
        }
    }
    return false;

  }

  static void SendReturnToProcess(Process process) throws IOException {
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            boolean flag = scanner.hasNext();
            while (flag) {
                String line = scanner.next(); // TODO: default delimiter is whitespace, so it reads by word, not line
                if (line.isEmpty()) {
                    continue;
                }

                if (line.contains("[none]:")) {
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                        writer.newLine();
                        writer.flush();
                        flag = false;
                    }
                }
            }
        }
    }

    private void MoveKeyFiles(final String keyName, final String destination) throws IOException {
        File dir = Paths.get(destination).toFile();
        File dpub = Paths.get(dir.getAbsolutePath(), keyName.concat(".pub")).toFile();
        File dkey = Paths.get(dir.getAbsolutePath(), keyName.concat(".key")).toFile();

        File location = Paths.get(System.getProperty("user.dir")).toFile();
        File pub = Paths.get(location.getAbsolutePath(), keyName.concat(".pub")).toFile();
        File key = Paths.get(location.getAbsolutePath(), keyName.concat(".key")).toFile();

        Files.move(pub.toPath(), dpub.toPath());
        Files.move(key.toPath(), dkey.toPath());
    }

    //TODO: convert to commandline
    public void createConstellationConfig(final String keyName, final String destination) throws IOException {
        File confFile = Paths.get(destination, keyName.concat(".conf")).toFile();
        if (!confFile.exists()) {
            String prefix = confFile.getParent() + File.separator + keyName;
            try (FileWriter writer = new FileWriter(confFile)) {
                String urlstring = CONSTELLATION_URL.endsWith("/") ? CONSTELLATION_URL.replaceFirst("(.*)" + "/" + "$", "$1" + "") : CONSTELLATION_URL;
                URL url = new URL(urlstring);
                writer.write(createQuorumConfig(keyName, url, Paths.get(destination)));
                writer.flush();
                LOG.info("created constellation config at " + confFile.getPath());
            }
        } else {
            LOG.info("reusing constellation config at " + confFile.getPath());
        }
    }

    public static String createQuorumConfig(final String name, final URL url, final Path storage) {
        String prefix = Paths.get(storage.toString(), name).toString();
        StringBuffer buffer = new StringBuffer();

        buffer.append("url = \"" + url + "\"");
        buffer.append("\n");
        buffer.append("port = " + url.getPort());
        buffer.append("\n");
        buffer.append("socket = \"" + prefix + ".ipc\"");
        buffer.append("\n");
        buffer.append("othernodes = []");
        buffer.append("\n");
        buffer.append("publickeys = [\"" + prefix + ".pub\"]");
        buffer.append("\n");
        buffer.append("privatekeys = [\"" + prefix + ".key\"]");
        buffer.append("\n");
        buffer.append("storage = \"dir:" + storage.toString() + File.separator + "constellation\"");

        return buffer.toString();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initQuorumBean();
    }

    private void initQuorumBean() {

        // setup needed paths
        String baseResourcePath = System.getProperty("eth.geth.dir");
        if (StringUtils.isBlank(baseResourcePath)) {
            baseResourcePath = FileUtils.getClasspathName("geth");
        }

        if (SystemUtils.IS_OS_LINUX) {
            LOG.debug("Using quorum for linux");
            setQuorumPath(expandPath(baseResourcePath, QUORUM_LINUX_COMMAND));
            setConstellationPath(expandPath(baseResourcePath, CONSTELLATION_LINUX_COMMAND));
            setTesseraPath(expandPath(baseResourcePath, TESSERA_LINUX_COMMAND_PATH));
            setKeyGen(expandPath(baseResourcePath, CONSTELLATION_LINUX_KEYGEN));
            setKeyGenParams(CONSTELLATION_LINUX_KEYGEN_PARAMS);
            setTesseraKeyGen(expandPath(baseResourcePath,TESSERA_LINUX_KEYGEN));
            setTesseraKeyParams(TESEERA_LINUX_KEYGEN_PARAMS);

        } else if (SystemUtils.IS_OS_MAC_OSX) {
            LOG.debug("Using quorum for mac");
            setQuorumPath(expandPath(baseResourcePath, QUORUM_MAC_COMMAND));
            setConstellationPath(expandPath(baseResourcePath, CONSTELLATION_MAC_COMMAND));
            setKeyGen(expandPath(baseResourcePath, CONSTELLATION_MAC_KEYGEN));
            setKeyGenParams(CONSTELLATION_MAC_KEYGEN_PARAMS);

        } else if ( (SystemUtils.IS_OS_WINDOWS) && (StringUtils.equalsIgnoreCase(EMBEDDED_NODE, "geth")) ) {
            // run GETH
            return;

        } else if (SystemUtils.IS_OS_WINDOWS) {
            LOG.error("Running on unsupported OS! Only Linux and Mac OS X are currently supported for Quorum, on Windoze, please run with -Dgeth.node=geth");
            throw new IllegalArgumentException("Running on unsupported OS! Only Linux and Mac OS X are currently supported for Quorum, on Windoze, please run with -Dgeth.node=geth");

        } else {
            LOG.error("Running on unsupported OS! Only Linux and Mac OS X are currently supported");
            throw new IllegalArgumentException("Running on unsupported OS! Only Linux and Mac OS X are currently supported");
        }

        File quorumExec = new File(getQuorumPath());
        if (!quorumExec.canExecute()) {
            quorumExec.setExecutable(true);
        }

        File constExec = new File(getConstellationPath());
        if (!constExec.canExecute()) {
            constExec.setExecutable(true);
        }

        File keyGenExec = new File(getKeyGen());
        if (!keyGenExec.canExecute()) {
            keyGenExec.setExecutable(true);
        }

    }

    public void setTesseraConfigPath(String destination){
      this.tesseraConfig =  destination;
    }

  /**
   * Create Tessera Configfile
   * @param keyName
   * @param tesseraConfigPath
   */
    public void createTesseraConfig(String keyName, String tesseraConfigPath,String tesseraUrl) {
      String prefix = Paths.get(tesseraConfigPath, keyName).toString();
      File confFile = Paths.get(tesseraConfigPath, "tessera" + ".json").toFile();
      List<String> peers = new ArrayList<String>();
      peers.add(tesseraUrl);
      List<String> alwaysSend = new ArrayList<>();

      if (confFile.exists()) {
        LOG.info("reusing tessera config at " + confFile.getPath());
        return;
      }
      try (FileOutputStream fileOutputStream = new FileOutputStream(confFile)) {
        URL url = new URL(tesseraUrl);
        JdbcConfig jdbcConfig = new JdbcConfig("", "", "jdbc:h2:mem:tessera");
        jdbcConfig.setAutoCreateTables(true);
        Config config = ConfigBuilder.create()
          .unixSocketFile(prefix + ".ipc")
          .useWhiteList(false)
          .jdbcConfig(jdbcConfig)
          .keyData(
            KeyDataBuilder.create()
              .withPublicKeys(Collections.singletonList(prefix + ".pub"))
              .withPrivateKeys(Collections.singletonList(prefix + ".key"))
              .build()
          )
          .peers(peers)
          .serverHostname(url.getProtocol() + "://" + url.getHost())
          .serverPort(url.getPort())
          .sslAuthenticationMode(SslAuthenticationMode.OFF)
          .sslClientTrustMode(SslTrustMode.TOFU)
          .sslClientTrustMode(SslTrustMode.TOFU)
          .build();


        System.setProperty("javax.xml.bind.context.factory","org.eclipse.persistence.jaxb.JAXBContextFactory");
        MarshallerBuilder.create().withoutBeanValidation().build().marshal(config, fileOutputStream);
        fileOutputStream.flush();
        LOG.info("created tessera config at " + confFile.getPath());
      } catch (IOException | JAXBException e) {
        LOG.error("Error occured while building tessera  config",e);
      }


  }





}
