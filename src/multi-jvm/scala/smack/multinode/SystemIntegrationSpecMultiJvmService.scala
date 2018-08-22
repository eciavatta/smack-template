package smack.multinode

import akka.remote.testkit.MultiNodeSpec
import smack.backend.ServiceSupervisor
import smack.common.traits.STMultiNodeSpec
import smack.common.utils.SystemIntegrationConfig._

class SystemIntegrationSpecMultiJvmService extends MultiNodeSpec(SystemIntegrationMultiNodeConfig, createActorSystem("service")) with STMultiNodeSpec {

  def initialParticipants: Int = roles.size

  "System integration test" should {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
      system.actorOf(ServiceSupervisor.props, ServiceSupervisor.name)
      enterBarrier("ready")
    }

    "wait the completion and terminate the test" in {
      enterBarrier("finished")
    }

  }
}
