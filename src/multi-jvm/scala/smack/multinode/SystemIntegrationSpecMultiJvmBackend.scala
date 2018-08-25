package smack.multinode

import akka.remote.testkit.MultiNodeSpec
import smack.backend.BackendSupervisor
import smack.commons.utils.SystemIntegrationConfig._
import smack.commons.traits.STMultiNodeSpec

class SystemIntegrationSpecMultiJvmBackend extends MultiNodeSpec(SystemIntegrationMultiNodeConfig, createActorSystem("backend")) with STMultiNodeSpec {

  def initialParticipants: Int = roles.size

  "System integration test" should {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
      system.actorOf(BackendSupervisor.props, BackendSupervisor.name)
      enterBarrier("ready")
    }

    "wait the completion and terminate the test" in {
      enterBarrier("finished")
    }

  }
}
