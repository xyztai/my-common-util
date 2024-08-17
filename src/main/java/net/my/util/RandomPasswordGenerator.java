package net.my.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class RandomPasswordGenerator {
    private static Random random = new Random();
    private static String specialChars = "[!@#$%^&*()_+{}|:<>?-]";

    private static char getUpperChar() {
        return (char) (random.nextInt(26) + 'A');
    }

    private static char getLowerChar() {
        return (char) (random.nextInt(26) + 'a');
    }

    private static char getNumberChar() {
        return (char) (random.nextInt(10) + '0');
    }

    private static char getSpecialChar() {
        return specialChars.charAt(random.nextInt(specialChars.length()));
    }

    public static String generateStrongPassword(int length, int typesCount) {
        int[] charTypeCount = new int[typesCount];
        int curLen = 0;
        int reserveLen = typesCount;
        for(int i = 0; i < typesCount - 1; i++) {
            reserveLen--;
            charTypeCount[i] = random.nextInt(length - curLen - reserveLen) + 1;
            curLen += charTypeCount[i];
        }
        charTypeCount[typesCount - 1] = length - curLen;
        StringBuilder sb = new StringBuilder();
        // log.info("charTypeCount: {}", charTypeCount);
        while(length > 0) {
            int index = random.nextInt(typesCount);
            int oldIndex = index;
            while(charTypeCount[index] == 0) {
                index = (index + 1) % typesCount;
                if(oldIndex == index)
                    break;
            }
            charTypeCount[index]--;
            char ch = 0;
            switch (index) {
                case 0:
                    ch = getUpperChar();
                    break;
                case 1:
                    ch = getLowerChar();
                    break;
                case 2:
                    ch = getNumberChar();
                    break;
                case 3:
                    ch = getSpecialChar();
                    break;
                default:
                    break;
            }
            sb.append(ch);
            length -= 1;
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        for(int i = 0; i < 20; i++){
            String strongPassword = generateStrongPassword(8, 4);
            log.info("Strong Password: {}", strongPassword);
        }
    }
}