public class BankAccount {
    private final String accountNumber;
    private double balance;
    private String ownerName;
    //Kỹ thuật nối tiếp constructor, thuận lợi cho người dùng, gán số dư ban đầu bằng 0
    public BankAccount(String accountNumber, String ownerName){
        this(accountNumber, ownerName, 0.0);
    }
    //Constructor 3 tham số
    public BankAccount(String accountNumber, String ownerName, double balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        if (balance < 0) {
            System.out.println("So du ban dau khong dc nho hon 0, da gan mac dinh balance=0");
            this.balance = 0;
        }else{
            this.balance=balance;
        }
    }
    public void deposit(double amount){
        //amount này không phải thuộc tính được khai báo ở đề bài nên không cần this
        if (amount <= 0){
            System.out.println("Loi: so tien nap vao phai > 0");
        }else{
            this.balance+=amount;
            System.out.println("Nap thanh cong " +amount+" VND");
        }
    }
    public boolean withdraw(double amount){
        if (0< amount && amount <= this.balance) {
            System.out.println("Da rut thanh cong " + amount + " VND");
            this.balance -= amount;
            return true;
        }else{
            System.out.println("Rut tien khong thanh cong");
            return false;
        }
    }
    public double getBalance(){
        return this.balance;
    }
}
