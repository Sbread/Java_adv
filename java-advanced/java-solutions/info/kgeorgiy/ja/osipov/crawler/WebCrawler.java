package info.kgeorgiy.ja.osipov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {

    private final Downloader downloader;
    private final ExecutorService extractorService;
    private final ExecutorService downloaderService;
    private final int perHost;
    private Map<String, HostTask> hostTasks;

    /**
     * Constructor for web-crawler
     *
     * @param downloader {@link Downloader} token for downloading pages
     * @param downloaders max pages to be downloaded parallel
     * @param extractors max pages from that parallel extracted links
     * @param perHost max pages to be downloaded parallel to each host
     */

    public WebCrawler(final Downloader downloader,
                      final int downloaders,
                      final int extractors,
                      final int perHost) {
        this.downloader = downloader;
        this.downloaderService = Executors.newFixedThreadPool(downloaders);
        this.extractorService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    /**
     * Downloads website up to specified depth.
     *
     * @param url   start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @return download result.
     */
    @Override
    public Result download(final String url, final int depth) {
        return download(url, depth, List.of(), false);
    }

    /**
     * Downloads website up to specified depth.
     *
     * @param url   start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @param hosts domains to follow, pages on another domains should be ignored.
     * @return download result.
     */
    @Override
    public Result download(String url, int depth, List<String> hosts) {
        return download(url, depth, hosts, true);
    }


    private class HostTask {
        private final Queue<Runnable> tasks;
        private final Semaphore semaphore;

        public HostTask() {
            this.tasks = new ArrayDeque<>();
            this.semaphore = new Semaphore(perHost);
        }

        private synchronized void releaseOrSubmit() {
            if (tasks.isEmpty()) {
                semaphore.release();
            } else {
                downloaderService.submit(tasks.poll());
            }
        }

        private synchronized void addTask(final Runnable task) {
            final Runnable runnable = () -> {
                task.run();
                releaseOrSubmit();
            };
            if (semaphore.tryAcquire()) {
                downloaderService.submit(runnable);
            } else {
                tasks.add(runnable);
            }
        }
    }

    private Result download(final String url,
                            final int depth,
                            final List<String> permittedHosts,
                            final boolean onlyPermitted) {
        final Map<String, IOException> exceptions = new ConcurrentHashMap<>();
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Set<String> prevURLs = ConcurrentHashMap.newKeySet();
        hostTasks = new ConcurrentHashMap<>();
        prevURLs.add(url);
        final Phaser phaser = new Phaser(1);
        while (phaser.getPhase() < depth) {
            Set<String> nextURLs = ConcurrentHashMap.newKeySet();
            Set<String> finalPrevURLs = prevURLs;
            prevURLs.forEach(u -> downloadURL(u, depth, permittedHosts, onlyPermitted,
                    phaser, exceptions, finalPrevURLs, nextURLs, downloaded));
            phaser.arriveAndAwaitAdvance();
            prevURLs = nextURLs;
        }
        return new Result(new ArrayList<>(downloaded), exceptions);
    }

    private void downloadURL(final String url,
                             final int depth,
                             final List<String> permittedHosts,
                             final boolean onlyPermitted,
                             final Phaser phaser,
                             final Map<String, IOException> exceptions,
                             final Set<String> prevURLs,
                             final Set<String> nextURLs,
                             final Set<String> downloaded) {
        try {
            final String host = URLUtils.getHost(url);
            if (onlyPermitted && !permittedHosts.contains(host)) {
                return;
            }
            hostTasks.putIfAbsent(host, new HostTask());
            final HostTask hostTask = hostTasks.get(host);
            phaser.register();
            hostTask.addTask(() -> {
                try {
                    final Document document = downloader.download(url);
                    downloaded.add(url);
                    if (phaser.getPhase() + 1 < depth) {
                        phaser.register();
                        final Runnable extractLinks = () -> {
                            try {
                                document.extractLinks()
                                        .stream()
                                        .filter(link -> !downloaded.contains(link)
                                                && !exceptions.containsKey(link)
                                                && !prevURLs.contains(link))
                                        .forEach(nextURLs::add);
                            } catch (IOException ignored) {

                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        };
                        extractorService.submit(extractLinks);
                    }
                } catch (IOException e) {
                    exceptions.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        } catch (MalformedURLException e) {
            exceptions.put(url, e);
        }
    }

    /**
     * Closes this web-crawler, relinquishing any allocated resources.
     */
    @Override
    public void close() {
        shutdownAndAwaitTermination(downloaderService);
        shutdownAndAwaitTermination(extractorService);
    }

    void shutdownAndAwaitTermination(ExecutorService pool) {
        try {
            if (!pool.awaitTermination(60, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.MILLISECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected 5 non-null args: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        try (final Crawler crawler = new WebCrawler(
                new CachingDownloader(1.0),
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4])
        )) {
            crawler.download(args[0], Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Expected numbers in arguments for depth, downloads, extractors and per host "
                    + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error in CachingDownloader occurred " + e.getMessage());
        }
    }
}
