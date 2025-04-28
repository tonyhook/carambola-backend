package cc.tonyhook.carambola.backend.service.ad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ServerRepository;
import cc.tonyhook.carambola.backend.entity.ad.Server;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.transaction.Transactional;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;

@Service
public class ServerService {

    private final ServerRepository serverRepository;

    @Value("${app.service.local:/home/ubuntu/}")
    private String localPath;
    @Value("${app.service.remote:/home/ubuntu/}")
    private String remotePath;

    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    public Server getServer(String address) {
        return serverRepository.findTopByAddress(address);
    }

    public Map<Integer, Integer> getServerStatus(String username) {
        if (username == null) {
            List<Server> serverList = serverRepository.findAll();

            Map<Integer, Integer> serverStatus = new HashMap<Integer, Integer>();
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = loggerContext.getLogger("net.schmizz");
            logger.setLevel(Level.ERROR);

            for (Server server : serverList) {
                try {
                    SSHClient client = new SSHClient();
                    client.addHostKeyVerifier(new PromiscuousVerifier());
                    client.setConnectTimeout(10);
                    client.connect(server.getAddress(), 22);
                    client.authPassword(server.getUsername(), server.getPassword());
                    client.close();

                    serverStatus.put(server.getNode(), 1);
                } catch (Exception e) {
                    serverStatus.put(server.getNode(), 0);
                }
            }

            return serverStatus;
        } else {
            return new HashMap<Integer, Integer>();
        }
    }

    public Boolean service(String username, Integer id, String service, String action) {
        if (username == null) {
            Server server = serverRepository.findById(id).orElse(null);

            if (server == null) {
                return false;
            }

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger logger = loggerContext.getLogger("net.schmizz");
            logger.setLevel(Level.ERROR);

            try {
                SSHClient client = new SSHClient();
                client.useCompression();
                client.addHostKeyVerifier(new PromiscuousVerifier());
                client.connect(server.getAddress(), 22);
                client.authPassword(server.getUsername(), server.getPassword());

                if (action.equals("deploy")) {
                    Session session = client.startSession();
                    Command cmd = session.exec("sudo systemctl stop " + service + ".service");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    client.newSCPFileTransfer()
                        .upload(new FileSystemFile(localPath + "carambola-" + service), remotePath + "carambola-" + service);
                    client.newSCPFileTransfer()
                        .upload(new FileSystemFile(localPath + service + ".yml"), remotePath + service + ".yml");

                    session = client.startSession();
                    cmd = session.exec("sudo chmod +x " + remotePath + "carambola-" + service);
                    reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    session = client.startSession();
                    cmd = session.exec("sudo systemctl start " + service + ".service");
                    reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    cmd.join();
                    session.close();

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    FileSystemFile file = new FileSystemFile(localPath + "carambola-" + service);
                    if (service.equals("serving")) {
                        server.setServingTimestamp(new Timestamp(System.currentTimeMillis()));
                        server.setServingVersion(df.format(new Timestamp(file.getLastModifiedTime() * 1000)));
                    }
                    if (service.equals("tracking")) {
                        server.setTrackingTimestamp(new Timestamp(System.currentTimeMillis()));
                        server.setTrackingVersion(df.format(new Timestamp(file.getLastModifiedTime() * 1000)));
                    }

                    serverRepository.save(server);
                }
                if (action.equals("revert")) {
                    Session session = client.startSession();
                    Command cmd = session.exec("sudo systemctl stop " + service + ".service");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    client.newSCPFileTransfer()
                        .upload(new FileSystemFile(localPath + "carambola-" + service + ".bak"), remotePath + "carambola-" + service);

                    session = client.startSession();
                    cmd = session.exec("sudo chmod +x " + remotePath + "carambola-" + service);
                    reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    session = client.startSession();
                    cmd = session.exec("sudo systemctl start " + service + ".service");
                    reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }

                    cmd.join();
                    session.close();

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    FileSystemFile file = new FileSystemFile(localPath + "carambola-" + service + ".bak");
                    if (service.equals("serving")) {
                        server.setServingTimestamp(new Timestamp(System.currentTimeMillis()));
                        server.setServingVersion(df.format(new Timestamp(file.getLastModifiedTime() * 1000)));
                    }
                    if (service.equals("tracking")) {
                        server.setTrackingTimestamp(new Timestamp(System.currentTimeMillis()));
                        server.setTrackingVersion(df.format(new Timestamp(file.getLastModifiedTime() * 1000)));
                    }

                    serverRepository.save(server);
                }
                if (action.equals("reboot")) {
                    Session session = client.startSession();
                    Command cmd = session.exec("sudo reboot");
                    cmd.getExitStatus();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                    while (reader.readLine() != null) {
                    }
                    cmd.join();
                    session.close();
                }

                client.disconnect();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public List<Server> getServerList(String username) {
        if (username == null) {
            List<Server> serverList = serverRepository.findAll();
            return serverList;
        } else {
            return new ArrayList<Server>();
        }
    }

    public Server getServer(String username, Integer id) {
        if (username == null) {
            return serverRepository.findById(id).orElse(null);
        } else {
            return null;
        }
    }

    public Server addServer(String username, Server newServer) {
        if (newServer != null && username == null) {
            newServer.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newServer.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Server updatedServer = serverRepository.save(newServer);

            return updatedServer;
        } else {
            return null;
        }
    }

    public Server updateServer(String username, Server targetServer, Server newServer) {
        if (targetServer != null && newServer != null && username == null) {
            newServer.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Server updatedServer = serverRepository.save(newServer);

            return updatedServer;
        } else {
            return null;
        }
    }

    @Transactional
    public Server removeServer(String username, Server targetServer) {
        if (targetServer != null && username == null) {
            serverRepository.delete(targetServer);

            return targetServer;
        } else {
            return null;
        }
    }

}
