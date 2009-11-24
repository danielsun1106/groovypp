package groovy.util.concurrent

import java.util.concurrent.ConcurrentLinkedQueue

/**
* @author ven
*/
@Typed
class DivideAndConquerProblemSolver {

  def DivideAndConquerProblemSolver(SelfRecurringProblem problem, int nWorkers) {
    workers = (1..nWorkers).map{ new Worker(it - 1) }.asList ()

    // Prevent work stealing happening immediately, distribute the work.
    root = new Job(problem, null)
    List<Job> jobs = [root]
    List<Job> atomics = []
    while (jobs.size() > 0 && jobs.size() < nWorkers) {
      jobs = removeAtomic(jobs, atomics)
      jobs = jobs.map{ job -> job.problem.sub().map{ new Job(it, job) } }.flatten().asList()
    }
    for (int i = 0; i < jobs.size(); ++i) workers[i % nWorkers].jobs.addFirst(jobs[i])
    for (int i = 0; i < atomics.size(); ++i) workers[i % nWorkers].jobs.addFirst(atomics[i])
  }

  def solve() {
    workers.map {
      def t = new Thread(it)
      t.start()
      t
    }.each { it.join() }
    root.result
  }

  List<Job> removeAtomic(List<Job> jobs, List atomics) {
    def result = []
    for (job in jobs) {
      (job.problem.complex() ? result : atomics) << job
    }
    result
  }

  class Job {
    final SelfRecurringProblem problem
    final Job parent
    volatile ConcurrentLinkedQueue<Job> children = []
    volatile Object result

    Job(SelfRecurringProblem problem, Job parent) {
      this.@problem = problem
      this.@parent = parent
      if (parent) parent.@children << this
    }

    void setResult(Object result) {
      this.@result = result
      assert parent != this
      if (parent) {
        synchronized (parent) {
          def children = parent.children
          if (children != null) {
            assert !children.isEmpty()
            if (!children.any{it.result == null}) {
              parent.result = problem.combine(children.map{it.result})
              parent.children = null
            }
          }
        }
      }
    }
  }

  class Worker implements Runnable{
    Worker(int num) { this.num = num }

    int num

    // TODO(ven): maybe can do non locking?
    class JobList extends Lockable {
      LinkedList<Job> list = []

      def void addFirst(Job e) {
        withLock {
          list.addFirst e
        }
      }

      Job removeFirst() {
        withLock {
          if (!list.isEmpty()) {
            return list.removeFirst()
          } else {
            return null
          }
        }
      }

      Job removeLast() {
        withLock {
          if (!list.isEmpty()) {
            return list.removeLast()
          } else {
            return null
          }
        }
      }
    }

    JobList jobs = []

    void run() {
      Job job = null
      while (true) {
        if ((job = jobs.removeFirst()) == null) {
          // Steal work from someone else.
          int nWorkers = workers.size()
          int i = (num + 1) % nWorkers
          while (i != num) {
            if ((job = workers[i].jobs.removeLast()) != null) break
            i = (i + 1) % nWorkers
          }
        }
        if (!job) break
        if (job.problem.complex()) {
          job.problem.sub().each{
            def child = new Job(it, job)
            jobs.addFirst(child)
          }
        } else {
          job.result = job.problem.solve()
        }
      }
    }
  }

  final List<Worker> workers
  final Job root
}