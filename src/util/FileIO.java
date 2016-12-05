package util;

import model.Actor;

import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * Created by yutakase on 2016/10/12.
 */
public final class FileIO {

    private FileIO() {
    }

    public static Optional<List<List<Actor>>> loadAgentLog(String fileName) {
        try {
            return Optional.of((List<List<Actor>>) openInputStream(fileName).readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static void writeActorLog(String fileName, List<List<Actor>> agentLogList) {
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = openOutputStream(fileName);
            objectOutputStream.writeObject(agentLogList);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ObjectInputStream openInputStream(String fileName) throws IOException {
        return new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)));
    }

    private static ObjectOutputStream openOutputStream(String fileName) throws IOException {
        return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    }
}
