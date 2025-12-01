package applet;

import javacard.framework.*;

/**
 * Module quản lý sách mượn
 * Xử lý thêm, lấy danh sách và trả sách
 */
public class BookManager {
    
    private byte[] borrowedBooks; // Mảng 1 chiều: [BOOK_1][BOOK_2]...
    private byte numBorrowedBooks;
    
    /**
     * Khởi tạo BookManager
     */
    public BookManager() {
        borrowedBooks = new byte[AppletConstants.MAX_BORROWED_BOOKS * AppletConstants.BOOK_ID_LENGTH];
        numBorrowedBooks = 0;
    }
    
    /**
     * Thêm sách mượn (yêu cầu xác thực PIN)
     * Format: [BOOK_ID]
     * 
     * @param apdu APDU command
     * @param pinManager PinManager để kiểm tra PIN đã xác thực
     */
    public void addBorrowedBook(APDU apdu, PinManager pinManager) {
        if (!pinManager.isPinValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        if (numBorrowedBooks >= AppletConstants.MAX_BORROWED_BOOKS) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        
        // Tính vị trí trong mảng 1 chiều: sách thứ i nằm ở vị trí i * BOOK_ID_LENGTH
        short bookOffset = (short)(numBorrowedBooks * AppletConstants.BOOK_ID_LENGTH);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, 
                      borrowedBooks, bookOffset, AppletConstants.BOOK_ID_LENGTH);
        numBorrowedBooks++;
    }
    
    /**
     * Lấy danh sách sách đang mượn
     * Response: [NUM_BOOKS][BOOK_ID_1][BOOK_ID_2]...
     * 
     * @param apdu APDU command
     */
    public void getBorrowedBooks(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short offset = 0;
        
        buffer[offset++] = numBorrowedBooks;
        
        // Copy từng book ID từ mảng 1 chiều
        for (byte i = 0; i < numBorrowedBooks; i++) {
            short bookOffset = (short)(i * AppletConstants.BOOK_ID_LENGTH);
            Util.arrayCopy(borrowedBooks, bookOffset, 
                          buffer, offset, AppletConstants.BOOK_ID_LENGTH);
            offset += AppletConstants.BOOK_ID_LENGTH;
        }
        
        apdu.setOutgoingAndSend((short)0, offset);
    }
    
    /**
     * Trả sách (yêu cầu xác thực PIN)
     * Format: [BOOK_ID]
     * 
     * @param apdu APDU command
     * @param pinManager PinManager để kiểm tra PIN đã xác thực
     */
    public void returnBook(APDU apdu, PinManager pinManager) {
        if (!pinManager.isPinValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        
        // Tìm sách cần trả trong mảng 1 chiều
        for (byte i = 0; i < numBorrowedBooks; i++) {
            short bookOffset = (short)(i * AppletConstants.BOOK_ID_LENGTH);
            
            if (Util.arrayCompare(buffer, ISO7816.OFFSET_CDATA, 
                                 borrowedBooks, bookOffset, 
                                 AppletConstants.BOOK_ID_LENGTH) == 0) {
                // Tìm thấy sách, xóa khỏi danh sách bằng cách dịch các sách sau lên
                for (byte j = i; j < (byte)(numBorrowedBooks - 1); j++) {
                    short srcOffset = (short)((j + 1) * AppletConstants.BOOK_ID_LENGTH);
                    short dstOffset = (short)(j * AppletConstants.BOOK_ID_LENGTH);
                    Util.arrayCopy(borrowedBooks, srcOffset,
                                  borrowedBooks, dstOffset, AppletConstants.BOOK_ID_LENGTH);
                }
                numBorrowedBooks--;
                return;
            }
        }
        
        // Không tìm thấy sách
        ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
    }
    
    /**
     * Lấy số lượng sách đang mượn
     * 
     * @return số lượng sách đang mượn
     */
    public byte getNumBorrowedBooks() {
        return numBorrowedBooks;
    }
}

