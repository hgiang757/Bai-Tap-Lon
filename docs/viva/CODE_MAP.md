# Bản đồ code hiện tại sau tối giản giao diện

Mốc 13/11/2026 (UTC+7). Sinh từ source thực tế, không lấy số dòng của báo cáo cũ.
Sau chuẩn hóa Google Style, liên kết dùng file và tên method để tránh số dòng lịch sử bị lệch. Xem [báo cáo Client](../FINAL_CLIENT_STABILITY_REPORT.md).

## Các điểm nên mở khi trình bày

| File | Vị trí và chữ ký hiện tại |
|---|---|
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public Entity() {` |
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public Entity(String id) {` |
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public Entity(String id, String name) {` |
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public String getId() {` |
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public String getName() {` |
| [server/model/User.java](../../src/main/java/com/auction/server/model/User.java) | `public abstract String getRoleName();` |
| [server/model/Item.java](../../src/main/java/com/auction/server/model/Item.java) | `public abstract String getCategory();` |
| [server/model/ItemFactory.java](../../src/main/java/com/auction/server/model/ItemFactory.java) | `public abstract Item create(String id, String name, String description, Seller owner);` |
| [server/model/ItemFactory.java](../../src/main/java/com/auction/server/model/ItemFactory.java) | `public Item create(String id, String name, String description, Seller owner) {` |
| [server/model/ItemFactory.java](../../src/main/java/com/auction/server/model/ItemFactory.java) | `public static ItemFactory forCategory(String category) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public long minimumNextBid() {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public void requireBidder(String userId, String role, LocalDateTime time) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public void requireMinimumBid(long amount) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public Auction withBid(String bidderId, long amount) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public long heldBy(String userId) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public void requireAutoBid(long maximum, long increment) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public void requireEditable() {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public void requireCancelable() {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public boolean hasExpired(LocalDateTime time) {` |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public boolean shouldStart(LocalDateTime time) {` |
| [server/dao/AuctionJsonMapper.java](../../src/main/java/com/auction/server/dao/AuctionJsonMapper.java) | `public static Auction auction(JsonObject json) {` |
| [server/dao/AuctionJsonMapper.java](../../src/main/java/com/auction/server/dao/AuctionJsonMapper.java) | `public static void applyBid(JsonObject target, Auction accepted, String username) {` |
| [server/dao/AuctionJsonMapper.java](../../src/main/java/com/auction/server/dao/AuctionJsonMapper.java) | `public static JsonObject bid(BidTransaction bid) {` |
| [server/service/AuctionService.java](../../src/main/java/com/auction/server/service/AuctionService.java) | `public synchronized JsonObject execute(` |
| [server/service/AuctionService.java](../../src/main/java/com/auction/server/service/AuctionService.java) | `private JsonObject commit(JsonObject before, JsonObject response) {` |
| [server/service/AuctionService.java](../../src/main/java/com/auction/server/service/AuctionService.java) | `public synchronized void tick() {` |
| [server/service/AuctionService.java](../../src/main/java/com/auction/server/service/AuctionService.java) | `private JsonElement handleAction(String action, String token, JsonObject user, JsonObject p) {` |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `JsonObject authenticatedUser(String token) {` |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `JsonObject createUser(` |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `JsonObject login(JsonObject p) {` |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `JsonObject register(JsonObject p) {` |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `JsonObject manageUser(JsonObject actor, JsonObject p, String action) {` |
| [server/service/ProductLogic.java](../../src/main/java/com/auction/server/service/ProductLogic.java) | `private JsonObject validateProduct(JsonObject p) {` |
| [server/service/ProductLogic.java](../../src/main/java/com/auction/server/service/ProductLogic.java) | `JsonObject createAuction(JsonObject user, JsonObject p) {` |
| [server/service/ProductLogic.java](../../src/main/java/com/auction/server/service/ProductLogic.java) | `JsonObject updateAuction(JsonObject user, JsonObject p) {` |
| [server/service/BiddingLogic.java](../../src/main/java/com/auction/server/service/BiddingLogic.java) | `JsonObject placeBid(JsonObject user, JsonObject p) {` |
| [server/service/BiddingLogic.java](../../src/main/java/com/auction/server/service/BiddingLogic.java) | `private void acceptBid(JsonObject a, JsonObject user, long amount, boolean automatic) {` |
| [server/service/BiddingLogic.java](../../src/main/java/com/auction/server/service/BiddingLogic.java) | `JsonObject setAuto(JsonObject user, JsonObject p) {` |
| [server/service/BiddingLogic.java](../../src/main/java/com/auction/server/service/BiddingLogic.java) | `private void processAutoBids(JsonObject a) {` |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `JsonObject wallet(JsonObject user) {` |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `long heldBalance(String userId) {` |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `long availableForAuction(JsonObject u, JsonObject a) {` |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `JsonObject transfer(JsonObject user, JsonObject p, boolean deposit) {` |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `void log(JsonObject user, String type, long amount, String auctionId) {` |
| [server/service/SettlementLogic.java](../../src/main/java/com/auction/server/service/SettlementLogic.java) | `void refreshAuctions() {` |
| [server/service/SettlementLogic.java](../../src/main/java/com/auction/server/service/SettlementLogic.java) | `private void settle(JsonObject a) {` |
| [server/service/SettlementLogic.java](../../src/main/java/com/auction/server/service/SettlementLogic.java) | `JsonObject cancel(JsonObject user, JsonObject p) {` |
| [server/service/AuctionContext.java](../../src/main/java/com/auction/server/service/AuctionContext.java) | `JsonObject snapshot(JsonObject a) {` |
| [server/service/AuctionContext.java](../../src/main/java/com/auction/server/service/AuctionContext.java) | `void event(String type, JsonObject auction, String userId) {` |
| [server/dao/AuctionRepository.java](../../src/main/java/com/auction/server/dao/AuctionRepository.java) | `public AuctionRepository(Path directory) throws IOException {` |
| [server/dao/AuctionRepository.java](../../src/main/java/com/auction/server/dao/AuctionRepository.java) | `public synchronized JsonObject load() throws IOException {` |
| [server/dao/AuctionRepository.java](../../src/main/java/com/auction/server/dao/AuctionRepository.java) | `public synchronized void save(JsonObject state) throws IOException {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `public static synchronized Server getInstance() throws IOException {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `private Server(String host, int port, Path data, boolean demo) throws IOException {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `public void start() {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `private void accept() {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `private void publish(JsonObject event) {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `void readLoop() {` |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `void writeLoop() {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `private JsonElement write(String action, JsonObject payload) {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `private JsonObject replay(JsonObject command) {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `public void registerBidListener(String id, Consumer<Bid> listener) {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `private void onEvent(JsonObject e) {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `public void close() throws IOException {` |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `public void close() {` |
| [client/service/server/JsonTransport.java](../../src/main/java/com/auction/client/service/server/JsonTransport.java) | `public JsonObject call(String action, String token, String commandId, JsonObject payload) {` |
| [client/service/server/JsonTransport.java](../../src/main/java/com/auction/client/service/server/JsonTransport.java) | `private void read(Connection c) {` |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `private void registerRealtimeObserver() {` |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `public void onBidReceived(Bid bid) {` |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `private void handlePlaceBid() {` |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `private void detachAuction() {` |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `public void dispose() {` |
| [client/navigation/ViewTasks.java](../../src/main/java/com/auction/client/navigation/ViewTasks.java) | `public <T> boolean submit(` |
| [client/navigation/ViewTasks.java](../../src/main/java/com/auction/client/navigation/ViewTasks.java) | `public void dispatch(Runnable action) {` |
| [test: client/fixture/MockDataStore.java](../../src/test/java/com/auction/client/fixture/MockDataStore.java) | `private MockDataStore() {` |
| [test: client/fixture/MockDataStore.java](../../src/test/java/com/auction/client/fixture/MockDataStore.java) | `public static MockDataStore getInstance() {` |

## Kiểm kê toàn bộ file Java production

| File | Khai báo có tên (kể cả nested) | Trách nhiệm |
|---|---|---|
| [client/App.java](../../src/main/java/com/auction/client/App.java) | `public class App {` | Entry point, giao thức dùng chung hoặc exception |
| [client/controller/AdminController.java](../../src/main/java/com/auction/client/controller/AdminController.java) | `public class AdminController implements DisposableView {` | Hai bảng quản trị người dùng/phiên, ViewTasks và kiểm tra vai trò |
| [client/controller/AuctionRoomController.java](../../src/main/java/com/auction/client/controller/AuctionRoomController.java) | `public class AuctionRoomController implements DisposableView {`<br>`private record RoomData(` | Điều phối JavaFX và gọi service qua ViewTasks |
| [client/controller/AuthController.java](../../src/main/java/com/auction/client/controller/AuthController.java) | `public class AuthController implements DisposableView {` | Điều phối JavaFX và gọi service qua ViewTasks |
| [client/controller/ContactController.java](../../src/main/java/com/auction/client/controller/ContactController.java) | `public class ContactController {`<br>`private record TeamMember(String name, String email) {}` | Đọc contacts.properties, sao chép email và feedback tại card |
| [client/controller/GuideController.java](../../src/main/java/com/auction/client/controller/GuideController.java) | `public class GuideController {` | Sáu hướng dẫn ngắn và điều hướng quay lại |
| [client/controller/HomeController.java](../../src/main/java/com/auction/client/controller/HomeController.java) | `public class HomeController implements DisposableView {`<br>`private static class CountdownEntry {` | Điều phối JavaFX và gọi service qua ViewTasks |
| [client/controller/SellerProductsController.java](../../src/main/java/com/auction/client/controller/SellerProductsController.java) | `public class SellerProductsController implements DisposableView {`<br>`private record Products(List<Auction> rows, Map<String, String> paths) {}` | Điều phối JavaFX và gọi service qua ViewTasks |
| [client/controller/WalletController.java](../../src/main/java/com/auction/client/controller/WalletController.java) | `public class WalletController implements DisposableView {` | Điều phối JavaFX và gọi service qua ViewTasks |
| [client/Main.java](../../src/main/java/com/auction/client/Main.java) | `public class Main extends Application {` | Entry point, giao thức dùng chung hoặc exception |
| [client/model/AdminUser.java](../../src/main/java/com/auction/client/model/AdminUser.java) | `public class AdminUser {` | DTO hiển thị |
| [client/model/Auction.java](../../src/main/java/com/auction/client/model/Auction.java) | `public class Auction implements Serializable {` | DTO hiển thị |
| [client/model/AuctionStatus.java](../../src/main/java/com/auction/client/model/AuctionStatus.java) | `public enum AuctionStatus {` | DTO hiển thị |
| [client/model/AutoBid.java](../../src/main/java/com/auction/client/model/AutoBid.java) | `public final class AutoBid {` | DTO hiển thị |
| [client/model/Bid.java](../../src/main/java/com/auction/client/model/Bid.java) | `public class Bid implements Serializable {` | DTO hiển thị |
| [client/model/Item.java](../../src/main/java/com/auction/client/model/Item.java) | `public class Item implements Serializable {` | DTO hiển thị |
| [client/model/ItemCategory.java](../../src/main/java/com/auction/client/model/ItemCategory.java) | `public enum ItemCategory {` | DTO hiển thị |
| [client/model/Transaction.java](../../src/main/java/com/auction/client/model/Transaction.java) | `public record Transaction(` | DTO hiển thị |
| [client/model/TransactionType.java](../../src/main/java/com/auction/client/model/TransactionType.java) | `public enum TransactionType {` | DTO hiển thị |
| [client/model/User.java](../../src/main/java/com/auction/client/model/User.java) | `public class User implements Serializable {` | DTO hiển thị |
| [client/model/UserRole.java](../../src/main/java/com/auction/client/model/UserRole.java) | `public enum UserRole {` | DTO hiển thị |
| [client/navigation/DisposableView.java](../../src/main/java/com/auction/client/navigation/DisposableView.java) | `public interface DisposableView {` | Điều hướng, tác vụ nền và vòng đời view |
| [client/navigation/ViewNavigator.java](../../src/main/java/com/auction/client/navigation/ViewNavigator.java) | `public final class ViewNavigator {`<br>`public enum Route {`<br>`private static final class View {` | Điều hướng, tác vụ nền và vòng đời view |
| [client/navigation/ViewTasks.java](../../src/main/java/com/auction/client/navigation/ViewTasks.java) | `public final class ViewTasks implements AutoCloseable {`<br>`public static final class NotSubmittedException extends RejectedExecutionException {` | Điều hướng, tác vụ nền và vòng đời view |
| [client/service/AuctionAdminService.java](../../src/main/java/com/auction/client/service/AuctionAdminService.java) | `public interface AuctionAdminService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/AuctionClientService.java](../../src/main/java/com/auction/client/service/AuctionClientService.java) | `public interface AuctionClientService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/AuctionRoomSnapshot.java](../../src/main/java/com/auction/client/service/AuctionRoomSnapshot.java) | `public record AuctionRoomSnapshot(Auction auction, List<Bid> history) {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/AutoBidException.java](../../src/main/java/com/auction/client/service/AutoBidException.java) | `public class AutoBidException extends RuntimeException {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/AutoBidService.java](../../src/main/java/com/auction/client/service/AutoBidService.java) | `public interface AutoBidService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/BidResult.java](../../src/main/java/com/auction/client/service/BidResult.java) | `public class BidResult {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/BidSubmissionTracker.java](../../src/main/java/com/auction/client/service/BidSubmissionTracker.java) | `public final class BidSubmissionTracker {`<br>`public enum Phase {`<br>`public record Key(String username, String auctionId) {}`<br>`public record Ticket(Key key, long command) {}`<br>`public record ReadStamp(Key key, long revision, Phase phase) {}`<br>`private static final class Entry {` | Hợp đồng service hoặc quy tắc dùng chung |
| [test: client/fixture/AutoBidEngine.java](../../src/test/java/com/auction/client/fixture/AutoBidEngine.java) | `public final class AutoBidEngine {` | Fixture chỉ dùng trong test, không đóng gói production |
| [test: client/fixture/MockAuctionClientService.java](../../src/test/java/com/auction/client/fixture/MockAuctionClientService.java) | `public class MockAuctionClientService implements AuctionClientService {` | Fixture chỉ dùng trong test, không đóng gói production |
| [test: client/fixture/MockAutoBidService.java](../../src/test/java/com/auction/client/fixture/MockAutoBidService.java) | `public final class MockAutoBidService implements AutoBidService {` | Fixture chỉ dùng trong test, không đóng gói production |
| [test: client/fixture/MockDataStore.java](../../src/test/java/com/auction/client/fixture/MockDataStore.java) | `public final class MockDataStore {`<br>`record Hold(String username, long amount) {}` | Fixture chỉ dùng trong test, không đóng gói production |
| [test: client/fixture/MockManagementService.java](../../src/test/java/com/auction/client/fixture/MockManagementService.java) | `public class MockManagementService` | Fixture chỉ dùng trong test, không đóng gói production |
| [test: client/fixture/MockWalletService.java](../../src/test/java/com/auction/client/fixture/MockWalletService.java) | `public final class MockWalletService implements WalletService {` | Fixture chỉ dùng trong test, không đóng gói production |
| [client/service/ProductService.java](../../src/main/java/com/auction/client/service/ProductService.java) | `public interface ProductService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/server/JsonTransport.java](../../src/main/java/com/auction/client/service/server/JsonTransport.java) | `public final class JsonTransport implements AutoCloseable {`<br>`private final class Connection {` | Adapter/socket, DTO mapping và đối soát commandId phía client |
| [client/service/server/ServerError.java](../../src/main/java/com/auction/client/service/server/ServerError.java) | `public final class ServerError extends IllegalStateException {` | Adapter/socket, DTO mapping và đối soát commandId phía client |
| [client/service/server/ServerMapper.java](../../src/main/java/com/auction/client/service/server/ServerMapper.java) | `final class ServerMapper {` | Adapter/socket, DTO mapping và đối soát commandId phía client |
| [client/service/server/ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | `public final class ServerServices`<br>`private record CommandLock(` | Adapter/socket, DTO mapping và đối soát commandId phía client |
| [client/service/ServiceRegistry.java](../../src/main/java/com/auction/client/service/ServiceRegistry.java) | `public final class ServiceRegistry {`<br>`private record Services(` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/UserAdminService.java](../../src/main/java/com/auction/client/service/UserAdminService.java) | `public interface UserAdminService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/WalletException.java](../../src/main/java/com/auction/client/service/WalletException.java) | `public class WalletException extends RuntimeException {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/WalletService.java](../../src/main/java/com/auction/client/service/WalletService.java) | `public interface WalletService {` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/service/WalletSnapshot.java](../../src/main/java/com/auction/client/service/WalletSnapshot.java) | `public record WalletSnapshot(` | Hợp đồng service hoặc quy tắc dùng chung |
| [client/ui/AppFooter.java](../../src/main/java/com/auction/client/ui/AppFooter.java) | `public final class AppFooter extends VBox {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/AppHeader.java](../../src/main/java/com/auction/client/ui/AppHeader.java) | `public final class AppHeader extends HBox {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/Icons.java](../../src/main/java/com/auction/client/ui/Icons.java) | `public final class Icons {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/PasswordFieldWithToggle.java](../../src/main/java/com/auction/client/ui/PasswordFieldWithToggle.java) | `public final class PasswordFieldWithToggle {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/ProductImages.java](../../src/main/java/com/auction/client/ui/ProductImages.java) | `public final class ProductImages {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/RoundedClip.java](../../src/main/java/com/auction/client/ui/RoundedClip.java) | `public final class RoundedClip {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/SmoothScroll.java](../../src/main/java/com/auction/client/ui/SmoothScroll.java) | `public final class SmoothScroll {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/ui/UserMenu.java](../../src/main/java/com/auction/client/ui/UserMenu.java) | `public final class UserMenu extends HBox {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/util/AppInfo.java](../../src/main/java/com/auction/client/util/AppInfo.java) | `public final class AppInfo {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/util/AuctionTime.java](../../src/main/java/com/auction/client/util/AuctionTime.java) | `public final class AuctionTime {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/util/FormatUtil.java](../../src/main/java/com/auction/client/util/FormatUtil.java) | `public final class FormatUtil {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [client/util/Validators.java](../../src/main/java/com/auction/client/util/Validators.java) | `public final class Validators {` | Thành phần UI hoặc tiện ích trình bày/validation |
| [server/dao/AuctionJsonMapper.java](../../src/main/java/com/auction/server/dao/AuctionJsonMapper.java) | `public final class AuctionJsonMapper {` | JSON v1 sang Auction và merge bid, bảo toàn trường mở rộng |
| [server/dao/AuctionRepository.java](../../src/main/java/com/auction/server/dao/AuctionRepository.java) | `public final class AuctionRepository implements AutoCloseable {` | Load/save JSON bền vững và lock dữ liệu |
| [server/exception/ApiException.java](../../src/main/java/com/auction/server/exception/ApiException.java) | `public class ApiException extends RuntimeException {` | Entry point, giao thức dùng chung hoặc exception |
| [server/model/Admin.java](../../src/main/java/com/auction/server/model/Admin.java) | `public class Admin extends User {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Art.java](../../src/main/java/com/auction/server/model/Art.java) | `public class Art extends Item {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Auction.java](../../src/main/java/com/auction/server/model/Auction.java) | `public record Auction(` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/AuctionStatus.java](../../src/main/java/com/auction/server/model/AuctionStatus.java) | `public enum AuctionStatus {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Bidder.java](../../src/main/java/com/auction/server/model/Bidder.java) | `public class Bidder extends User {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/BidTransaction.java](../../src/main/java/com/auction/server/model/BidTransaction.java) | `public record BidTransaction(` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Electronics.java](../../src/main/java/com/auction/server/model/Electronics.java) | `public class Electronics extends Item {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Entity.java](../../src/main/java/com/auction/server/model/Entity.java) | `public abstract class Entity implements Serializable {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Item.java](../../src/main/java/com/auction/server/model/Item.java) | `public abstract class Item extends Entity {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/ItemFactory.java](../../src/main/java/com/auction/server/model/ItemFactory.java) | `public abstract class ItemFactory {`<br>`private static final class CategoryItem extends Item {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Seller.java](../../src/main/java/com/auction/server/model/Seller.java) | `public class Seller extends User {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/User.java](../../src/main/java/com/auction/server/model/User.java) | `public abstract class User extends Entity {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/UserRole.java](../../src/main/java/com/auction/server/model/UserRole.java) | `public enum UserRole {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/model/Vehicle.java](../../src/main/java/com/auction/server/model/Vehicle.java) | `public class Vehicle extends Item {` | Domain; xem OOP_EXPLANATION để phân biệt vai trò production và API cũ |
| [server/network/Server.java](../../src/main/java/com/auction/server/network/Server.java) | `public final class Server implements AutoCloseable {`<br>`private final class Client implements AutoCloseable {` | TCP request handler, subscriptions, queue và scheduler |
| [server/service/AntiSnipingPolicy.java](../../src/main/java/com/auction/server/service/AntiSnipingPolicy.java) | `public final class AntiSnipingPolicy {` | Hợp đồng service hoặc quy tắc dùng chung |
| [server/service/AuctionContext.java](../../src/main/java/com/auction/server/service/AuctionContext.java) | `final class AuctionContext {` | Snapshot JSON, thời gian, projection và buffer sự kiện dùng chung |
| [server/service/AuctionService.java](../../src/main/java/com/auction/server/service/AuctionService.java) | `public final class AuctionService implements AutoCloseable {` | Facade duy nhất giữ monitor, commandId, commit/rollback và publish |
| [server/service/AuctionStateValidator.java](../../src/main/java/com/auction/server/service/AuctionStateValidator.java) | `final class AuctionStateValidator {` | Kiểm tra cấu trúc và liên kết snapshot trước khi phục vụ |
| [server/service/AuthLogic.java](../../src/main/java/com/auction/server/service/AuthLogic.java) | `final class AuthLogic {`<br>`private record Session(String userId, Instant expires) {}` | Credentials, sessions, đăng ký và quản trị tài khoản |
| [server/service/BiddingLogic.java](../../src/main/java/com/auction/server/service/BiddingLogic.java) | `final class BiddingLogic {` | Bid thủ công/tự động, một luồng acceptBid và anti-sniping |
| [server/service/Passwords.java](../../src/main/java/com/auction/server/service/Passwords.java) | `final class Passwords {` | Hợp đồng service hoặc quy tắc dùng chung |
| [server/service/ProductLogic.java](../../src/main/java/com/auction/server/service/ProductLogic.java) | `final class ProductLogic {` | Sản phẩm, danh mục, lịch đấu giá, ảnh và quyền seller |
| [server/service/SettlementLogic.java](../../src/main/java/com/auction/server/service/SettlementLogic.java) | `final class SettlementLogic {` | Mở/đóng phiên, thanh toán một lần và hủy của admin |
| [server/service/WalletLogic.java](../../src/main/java/com/auction/server/service/WalletLogic.java) | `final class WalletLogic {` | Tiền giữ/khả dụng, nạp/rút và lịch sử ví trong transaction |
| [shared/JsonProtocol.java](../../src/main/java/com/auction/shared/JsonProtocol.java) | `public final class JsonProtocol {` | Entry point, giao thức dùng chung hoặc exception |

## Các phương thức kiểm thử

Một method parameterized có thể tạo nhiều test case trong Surefire. Số test chạy thực tế xem báo cáo tối giản.

| File | Method kiểm thử |
|---|---|
| [ClientUiTest.java](../../src/test/java/com/auction/client/controller/ClientUiTest.java) | `registerLoginSearchAndNavigation`<br>`bidHistoryChartAndRepeatedRoomLifecycle`<br>`allRoutesLoadWithAuthorizedRoles`<br>`mockAutoBidValidationIsKnownRejectionAndCanBeCorrected`<br>`queueRejectionRestoresBidUiWithoutPretendingItWasSent`<br>`timeoutAndPendingOrFailedReadNeverUnlockOrResend`<br>`responseRequiresReadStartedAfterResponseAndReadFailureKeepsLock`<br>`leavingOrChangingRoomCannotForgetInFlightCommandOrApplyOldCallback`<br>`homeShowsEveryRunningAndOnlyOpenUpcomingWithoutDuplicates`<br>`homeDistinguishesLoadingFailureEmptyAndFilteredResults`<br>`walletShowsFullLongAmountAndExactTooltipAtBothWidths`<br>`authStylesAreAppliedAndRegisterScrollsOnSmallWindow`<br>`accountMenuToggleStaysInSceneAndLeavesOtherControlsUsable`<br>`accountMenuCleansUpOnOwnerHideHeaderRemovalAndLogout`<br>`conciseGuideAndContactCopyWorkAtNarrowWidth`<br>`accountMenuNavigationAndRepeatedUseDoNotDuplicateContent` |
| [ImageTasksTest.java](../../src/test/java/com/auction/client/navigation/ImageTasksTest.java) | `blockedAndFullImageQueueDoesNotBlockBusinessCommands`<br>`closingImageScopeRemovesQueuedWorkAndInterruptsOnlyItsRunningRead`<br>`changingFilterOrReusingCellDropsOldQueuedAndAlreadyDeliveredImages`<br>`oneFailedImageDoesNotDisableLaterLoads`<br>`rejectedBusinessWorkReportsNotSentAndAcceptedMutationSurvivesClose` |
| [ViewTasksTest.java](../../src/test/java/com/auction/client/navigation/ViewTasksTest.java) | `lateReadCannotReplaceNewerResult`<br>`leavingViewSuppressesAlreadyQueuedCallbacksAndObserverEvents`<br>`leavingViewDoesNotCancelOrRetrySubmittedMutation`<br>`failureGoesToUiAndClosedScopeRejectsNewWork` |
| [BidSubmissionTrackerTest.java](../../src/test/java/com/auction/client/service/BidSubmissionTrackerTest.java) | `timeoutStaysUnknownEvenAfterSuccessfulFreshHistoryRead`<br>`readStartedBeforeCommandOrBeforeResponseCannotUnlock`<br>`failedReadDoesNotUnlockAndOldCommandCannotChangeNewCommand`<br>`unknownIsIsolatedByUserAndAuctionAndOnlyQueueRejectionMeansNotSent` |
| [MockAuctionBidAccessTest.java](../../src/test/java/com/auction/client/service/MockAuctionBidAccessTest.java) | `guestAndSellerCannotBid` |
| [MockAuthIntegrationTest.java](../../src/test/java/com/auction/client/service/MockAuthIntegrationTest.java) | `registeredAccountSharesManagementState`<br>`sampleLoginUsesTheSameStoreAsManagement` |
| [MockAutoBidServiceTest.java](../../src/test/java/com/auction/client/service/MockAutoBidServiceTest.java) | `earlierRegistrationWinsEqualMaximumAndNeverExceedsIt`<br>`earlierRegistrationWithoutBudgetDoesNotBlockAnAffordableRival`<br>`noSelfOutbiddingAndStopsAtCeiling`<br>`notifiesListenersOnOtherServiceInstances`<br>`validatesLimitsAndCancellation`<br>`concurrentManualBidsRemainMonotonicAndHoldOneWinner` |
| [MockManagementServiceTest.java](../../src/test/java/com/auction/client/service/MockManagementServiceTest.java) | `adminCannotLockOrDeleteSelf`<br>`cancellationRequiresReasonAndOnlyAllowsOpenOrRunning`<br>`sellerCannotDeleteBidOnOrOtherSellerAuction` |
| [MockSharedStoreTest.java](../../src/test/java/com/auction/client/service/MockSharedStoreTest.java) | `productChangesAndStatusesAreVisibleToAuctionService`<br>`categoriesRecognizeLegacyAndCurrentNames` |
| [MockWalletServiceTest.java](../../src/test/java/com/auction/client/service/MockWalletServiceTest.java) | `depositWithdrawAndValidation`<br>`holdsReleaseAndPaymentAreAtomic`<br>`cancellationReleasesFunds`<br>`concurrentDepositsDoNotLoseUpdates` |
| [ServiceRegistryTest.java](../../src/test/java/com/auction/client/service/ServiceRegistryTest.java) | `unsupportedModeDoesNotFallBackToMock`<br>`replacingOnlyAuctionCannotSilentlyKeepAnotherTcpSession` |
| [ProductImagesIoTest.java](../../src/test/java/com/auction/client/ui/ProductImagesIoTest.java) | `canceledAndExpiredLocalReadStopsWithoutSleeping`<br>`stalledHttpBodyHasReadTimeoutAndDoesNotWaitForever` |
| [ProductImagesTest.java](../../src/test/java/com/auction/client/ui/ProductImagesTest.java) | `saoChepAnhVaXuLyDuongDanSai` |
| [UiResourcesTest.java](../../src/test/java/com/auction/client/ui/UiResourcesTest.java) | `decorativeResourcesAreAbsentAndProductSupportRemains` |
| [AuctionTimeTest.java](../../src/test/java/com/auction/client/util/AuctionTimeTest.java) | `parsesStrictTime`<br>`summarizesDuration`<br>`validatesBounds` |
| [FormatUtilTest.java](../../src/test/java/com/auction/client/util/FormatUtilTest.java) | `walletLongAmountsKeepEveryDigit`<br>`concurrentCurrencyAndChartFormattingKeepsExactValues` |
| [ValidatorsTest.java](../../src/test/java/com/auction/client/util/ValidatorsTest.java) | `acceptsValidEmails`<br>`rejectsInvalidEmails`<br>`rejectsOversizedEmails`<br>`acceptsUsernames`<br>`rejectsUsernames`<br>`acceptsPasswords`<br>`rejectsPasswords`<br>`acceptsMatchingPasswords`<br>`rejectsMismatchedPasswords` |
| [FullStackTest.java](../../src/test/java/com/auction/integration/FullStackTest.java) | `sellerUploadsImageBidderReadsItAndAdminManagesProduct`<br>`sellerValidationIdentifiesTheRejectedFieldAndLeavesProductsUnchanged`<br>`twoAdaptersReceiveLiveBidWalletAndAutoBidWithoutMock`<br>`failingBidObserverDoesNotSuppressOtherObserversOrGeneralChanges`<br>`registrationLoginDepositWithdrawAndAdminLockReachBackend`<br>`pendingCommandSurvivesClientRestartAndIsAppliedOnlyOnce`<br>`unavailableServerDoesNotReturnMockData`<br>`reconnectAfterServerRestartRequiresLoginAndRetainsMoney`<br>`coldServerJvmCanAuthenticateWithinTheNormalClientTimeout`<br>`requestTimeoutClosesConnectionWhenPeerStopsReading`<br>`lostBidAcknowledgementRetriesTheSameCommandNotAnotherBid` |
| [ServerUiIntegrationTest.java](../../src/test/java/com/auction/integration/ServerUiIntegrationTest.java) | `javafxScreensOperateOnTheRealServer` |
| [AuctionJsonMapperTest.java](../../src/test/java/com/auction/server/dao/AuctionJsonMapperTest.java) | `readsLegacyAuctionWithoutMutatingHistoryImagesOrExtensionFields`<br>`applyingDomainBidUpdatesOnlyTheLeaderPriceAndCount`<br>`writesExactLegacyBidFieldsAndIntegerMoney` |
| [AuctionRepositoryTest.java](../../src/test/java/com/auction/server/dao/AuctionRepositoryTest.java) | `committedSnapshotSurvivesRestartAndIgnoresUncommittedTemporaryFile`<br>`secondRepositoryCannotWriteTheSameDataDirectory`<br>`corruptFileIsNeverResetOrOverwritten`<br>`writeFailurePreservesCommittedFileAndCanBeRetried`<br>`savingRequiresSuccessfulLoadAndAnOpenRepository` |
| [AuctionTest.java](../../src/test/java/com/auction/server/model/AuctionTest.java) | `minimumStepAppliesToFirstBidAndAllowsLargerJumps`<br>`participantRoleAndOwnershipAreEnforcedOnServer`<br>`startIsInclusiveAndEndIsExclusive`<br>`onlyRunningStatusAcceptsBids`<br>`acceptedBidChangesOneLeaderWithoutMutatingThePreviousSnapshot`<br>`settledAuctionsKeepTheirWinnerButReleaseTheHold`<br>`autoBidRequiresBothTheMinimumIncrementAndTheNextPrice`<br>`editCancelAndSchedulerRulesRespectTypedStatus`<br>`evenAnOpenAuctionCannotBeEditedOnceItHasBidHistory` |
| [ItemFactoryTest.java](../../src/test/java/com/auction/server/model/ItemFactoryTest.java) | `createsCorrectCategoryAndPreservesProductAndOwner`<br>`rejectsUnknownCategoryInsteadOfSilentlyCreatingOther`<br>`acceptsCategoryAliasesIntroducedOnMain`<br>`userRoleDispatchUsesTheRuntimeSubclass` |
| [ServerIntegrationTest.java](../../src/test/java/com/auction/server/network/ServerIntegrationTest.java) | `twoRealSocketsReceivePushAndReconnectReplaysOnlyOneBid`<br>`simultaneousEqualBidsCommitOnceAndMalformedClientDoesNotStopServer`<br>`oversizedFrameIsRejectedBeforeAllocation` |
| [AntiSnipingPolicyTest.java](../../src/test/java/com/auction/server/service/AntiSnipingPolicyTest.java) | `bidFarFromEnd_doesNotExtend`<br>`bidInLastSeconds_extends`<br>`bidExactlyAtThreshold_extends`<br>`bidAfterEnd_doesNotExtend`<br>`invalidConfig_throws` |
| [AuctionServiceTest.java](../../src/test/java/com/auction/server/service/AuctionServiceTest.java) | `credentialsAreHashedAndPublicDtosAreSafe`<br>`registerValidatesRoleUniquenessAndPassword`<br>`walletHoldsOutbidReleaseAndWithdrawAreConsistent`<br>`firstBidRequiresStepAndIntegerAndSellerCannotBidOwnProduct`<br>`scheduledAuctionsCannotReceiveEarlyBidsAndOpenAutomatically`<br>`settlementPaysSellerExactlyOnceAndSurvivesRestart`<br>`antiSnipingExtendsAndDoesNotSettleAtOldDeadline`<br>`canceledAuctionReleasesHoldAndStopsAutoBid`<br>`autoBidRespectsLimitBudgetAndRegistrationOrder`<br>`earlierAutoBidCannotBlockAnAffordableRivalAfterItsBudgetChanges`<br>`idempotencyRejectsChangedPayloadAndPersistsFinalOutcome`<br>`concurrentAuctionsCannotOverspendOneWallet`<br>`equalConcurrentBidsChooseOneLeaderAndRejectTheOtherAtTheNewPrice`<br>`adminLockAppliesToExistingSessionAndCannotBeSpoofed`<br>`productCrudChecksOwnerAndPreservesAuctionHistory`<br>`storageFailureRollsBackMoneyAndCommandResult`<br>`failedBidDoesNotPublishOrPersistAndSameCommandCanBeRetried`<br>`noBidAuctionFinishesWithoutPaymentAndInvalidImagesAreRejected`<br>`extensionFieldsSurviveAutomaticBidsSettlementAndRestart`<br>`failedSettlementSaveRestoresBothWalletsAndRetryPaysExactlyOnce`<br>`rejectedProductUpdateDoesNotLeaveHelpersUsingTheDiscardedSnapshot` |
| [AuctionStateValidationTest.java](../../src/test/java/com/auction/server/service/AuctionStateValidationTest.java) | `incompleteSnapshotFailsStartupWithoutResetAndReleasesLock`<br>`corruptNestedDataFailsBeforeAnySchedulerOrWrite`<br>`inconsistentFinancialSnapshotIsRejectedWithoutChangingBytes` |

## Điểm mới và test hồi quy Client

| File | Method / trách nhiệm |
|---|---|
| [AuctionBidHistoryView.java](../../src/main/java/com/auction/client/ui/AuctionBidHistoryView.java) | Constructor/setupChart: cấu hình chart và placeholder; render: snapshot → chart/history; clear: đổi phòng; addPricePoint/attachPointTooltip/updateChartAxes: vẽ |
| [ServiceRegistry.java](../../src/main/java/com/auction/client/service/ServiceRegistry.java) | validateMode: chỉ Server, fixture tiêm qua setServices trong test; close: đóng adapter và giải phóng bộ dịch vụ |
| [ClientUiTest.java](../../src/test/java/com/auction/client/controller/ClientUiTest.java) | oldRecoveryCannotReconcileAnotherRoomUnknownCommand; historySnapshotOrdersCapsAndReplacesPointsAndExtensionClearsUrgency |
| [ServiceRegistryTest.java](../../src/test/java/com/auction/client/service/ServiceRegistryTest.java) | absentModeUsesOnlyServerAdaptersAndFailureNeverEnablesMock |
| [ServerUiIntegrationTest.java](../../src/test/java/com/auction/integration/ServerUiIntegrationTest.java) | uiCreatedAccountsProductsBidsWalletAndImageSurviveNonDemoRestart: đăng ký/form/ví/bid/auto-bid/restart không seed |
| [ServerServices.java](../../src/main/java/com/auction/client/service/server/ServerServices.java) | response: LOGIN/REGISTER không đăng ký feed bằng phiên cũ; FullStackTest.loginAfterRestartDoesNotRequireAnExpiredSessionReadFirst kiểm tra |

## Seller/Mock cập nhật 13/11/2026

Các số dòng bảng cũ là mốc lịch sử; tìm bằng tên method sau chỉnh sửa.
SellerProductsController.form/closeForm: listPane ↔ editorHost trong seller-products-view.fxml.
HomeController.applyFilters/buildAuctionCard: tab lịch sử và một badge trạng thái.
ServiceRegistry: TCP duy nhất, kiểm tra cùng adapter/session. FixtureServices thuộc src/test.
Server.getInstance: main không seed; constructor private; TestServers.create chỉ ở src/test mở bằng reflection.
ClientUiTest.sellerPaneStaysInMainWindowAndCleansUpOnCancelEscapeAndNavigation,
sellerSaveFailureKeepsDraftForCorrectionWithoutExtraWindow và
homeSeparatesHistoryAndPreservesSearchCategoryAndStatusFilters là test bổ sung.
ServerUiIntegrationTest còn kiểm settlement và restart của phiên tạo qua form thật.

## UI/UX ngày 14/11/2026

- [SellerProductsController](../../src/main/java/com/auction/client/controller/SellerProductsController.java): form, saveProduct, closeForm; điều phối list/quyền/service.
- [SellerProductFormView](../../src/main/java/com/auction/client/ui/SellerProductFormView.java): Draft, refresh, setBusy, showSaveError, close; nạp [FXML form](../../src/main/resources/com/auction/client/views/seller-product-form.fxml).
- [AuctionScheduleInput](../../src/main/java/com/auction/client/ui/AuctionScheduleInput.java): selection, validationMessage, parseDate; nhập lịch theo giờ Server.
- [ProductImagePicker](../../src/main/java/com/auction/client/ui/ProductImagePicker.java): select, loadPreview, close; task ảnh tách khỏi nghiệp vụ.
- [ViewNavigator](../../src/main/java/com/auction/client/navigation/ViewNavigator.java): prepareDialog; owner và listener lifecycle.
- [ProductImages](../../src/main/java/com/auction/client/ui/ProductImages.java): imageDirectory hỗ trợ auction.client.images để cô lập ảnh QA, mặc định giữ nguyên.

Chi tiết thay đổi và kết quả hiện tại: [FINAL_UI_UX_REVIEW](../FINAL_UI_UX_REVIEW.md).

## Đối chiếu thang điểm cuối 14/11/2026

Xem [báo cáo theo đề](../TEACHER_REQUIREMENTS_FINAL_AUDIT.md) và
[hướng dẫn học theo source](TEACHER_REQUIREMENTS_GUIDE.md).
`ServerStartupTest.productionConstructionIsPrivate` kiểm tra API tạo Server;
`concurrentlyGetInstance` cho 16 luồng cùng tạo lần đầu bằng getInstance.
`src/test/java/com/auction/server/network/TestServers.create` là helper test,
không đóng gói vào production; các integration test giữ cổng/thư mục riêng.

Các tên nội bộ theo Google Style: AuctionRoomController.timeAxis/priceAxis khớp fx:id
trong FXML; AuctionBidHistoryView dùng cùng tên; AuthController.updateModeUi;
ServerMapper.text/longValue thay s/n. Không đổi key JSON hay action TCP.

Ba exception theo hướng dẫn PDF tr. 3 ở server/exception: InvalidBidException,
AuctionClosedException, AuthenticationException kế thừa ApiException; Auction ném
hai loại đầu, AuthLogic.login ném loại thứ ba. CustomExceptionTest kiểm exception
thực tế và mã lỗi TCP cũ. Đây là ba file production mới, không phải fixture.

## Menu tài khoản sau tối giản

UserMenu.openMenu/hideMenu/isOpen: gắn/gỡ ScrollPane vào StackPane gốc của Scene;
parent của pane quyết định trạng thái mở, không có Popup/focus/ESC handler riêng.
ViewNavigator.loadView tạo container chính; disposeCurrentView và WINDOW_HIDDEN
đóng menu. setPrimaryStage tháo handler ở owner cũ trước khi đăng ký owner mới.
AppHeader giữ Observer cập nhật ví, bỏ timer Mock không thể được production gọi.
AuthController bỏ updateFeatureVisibility và các label/icon quảng bá, giữ validation,
remembered username, mật khẩu, vai trò và tác vụ login/register.
